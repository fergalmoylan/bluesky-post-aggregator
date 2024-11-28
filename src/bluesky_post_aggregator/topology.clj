(ns bluesky-post-aggregator.topology
  (:require [jackdaw.streams :as js]
            [jackdaw.serdes.edn :as ednserde])
  (:import (java.time Duration LocalDate)
           (java.time.format DateTimeFormatter)
           (org.apache.kafka.streams.kstream TimeWindows)))

(defn update-top-k [top-k-map key count k]
  (let [updated-map (update top-k-map key (fnil + 0) count)
        sorted-entries (sort-by val > updated-map)]
    (into {} (take k sorted-entries))))

(defn top-k-aggregator [agg-val new-val k]
  (reduce (fn [acc key]
            (update-top-k acc key 1 k))
          agg-val
          new-val))

(defn get-most-frequent-occurrences
  [frequency-map
   n]
  (mapv (fn [[k v]]
          {:key   (name k)
           :count v})
        (->> frequency-map
             (sort-by val >)
             (take n)
             (into {}))))

(defn split-record-by-value
  [input-stream
   cluster-key
   aggregation-fields]
  (-> input-stream
      (js/flat-map
        (fn [[_ v]]
          (for [cluster-key (cluster-key v)]
            [cluster-key
             (reduce
               (fn [value agg-key]
                 (assoc value
                   agg-key
                   (agg-key v)))
               {}
               aggregation-fields)])))))

(defn add-aggregate-counts
  [aggregated-record
   new-record
   aggregation-fields]
  (let [updated-aggregation-fields
        (reduce
          (fn [updated agg-key]
            (assoc updated
              agg-key
              (top-k-aggregator (agg-key aggregated-record)
                                (agg-key (second new-record))
                                10)))
          {:count (:count aggregated-record)}
          aggregation-fields)]
    (update updated-aggregation-fields
            :count
            (fnil inc 0))))

(defn format-record-for-opensearch
  [windowed-key
   aggregated-value
   output-keyname
   aggregation-fields]
  (let [main-key (.key windowed-key)
        window-start (.start (.window windowed-key))
        record-key (str main-key "-" window-start)]
    [record-key (reduce
                  (fn [value agg-key]
                    (assoc
                      value
                      (str "top-" (name agg-key))
                      (get-most-frequent-occurrences
                        (agg-key aggregated-value)
                        5)))
                  {:count (:count aggregated-value)
                   :timestamp (.start (.window windowed-key))
                   :index-name (str (name output-keyname) "-clusters-"
                                    (.format
                                      (LocalDate/now)
                                      (DateTimeFormatter/ofPattern "yyyy-MM-dd")))
                   output-keyname (.key windowed-key)}
                  aggregation-fields)]))

(defn record-aggregation-stream
  [input-stream
   output-topic
   cluster-key
   output-keyname
   aggregation-fields
   min-count-threshold]
  (-> input-stream
      (js/filter
        (fn [[_ v]]
            (seq (cluster-key v))))
      (split-record-by-value cluster-key aggregation-fields)
      (js/group-by-key
        {:key-serde   (ednserde/serde)
         :value-serde (ednserde/serde)})
      (js/window-by-time (TimeWindows/of
                           (Duration/ofMinutes 10)))
      (js/aggregate
        (constantly (reduce
                      (fn [value agg-key]
                        (assoc value agg-key {}))
                      {:count 0}
                      aggregation-fields))
        (fn [agg-val new-val]
          (add-aggregate-counts agg-val
                                new-val
                                aggregation-fields))
        {:topic-name (str (name cluster-key)
                          "-aggregate-store")
         :key-serde   (ednserde/serde)
         :value-serde (ednserde/serde)})
      (js/filter
        (fn [[_ v]]
          (< min-count-threshold
             (:count v))))
      (js/suppress {:max-records 1000
                    :max-bytes (* 1024 1024)
                    :until-time-limit-ms 60000})
      (js/to-kstream)
      (js/map
        (fn [[windowed-key aggregated-value]]
          (format-record-for-opensearch windowed-key
                                        aggregated-value
                                        output-keyname
                                        aggregation-fields)))
      ;(js/peek (fn [stream] (println stream)))
      (js/to output-topic)))

(defn topology-builder
  [builder
   app-config]
  (let [topic-config (get app-config "topic-config")
        input-topic (:input topic-config)
        output_topic (:output topic-config)
        input-stream (js/kstream builder input-topic)]
    (record-aggregation-stream input-stream
                               output_topic
                               :languages
                               :language
                               [:hashtags :hostnames]
                               2)
    (record-aggregation-stream input-stream
                               output_topic
                               :hashtags
                               :hashtag
                               [:languages :hostnames]
                               5)
    (record-aggregation-stream input-stream
                               output_topic
                               :hostnames
                               :hostname
                               [:hashtags :languages]
                               2)
    (record-aggregation-stream input-stream
                               output_topic
                               :urls
                               :url
                               []
                               0))
  builder)


