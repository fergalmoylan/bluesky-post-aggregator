(ns bluesky-post-aggregator.core
  (:require [environ.core :refer [env]]
            [jackdaw.serdes.json :as jsonserde]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]))

(defn -main
  [& args]
  (let [app-config {"bootstrap.servers" (env :kafka-addresses)
                    "application.id" (env :application-id)
                    "auto.offset.reset" "earliest"
                    "cache.max.bytes.buffering" "100000"
                    "topic-config" {:input {:topic-name (env :input-topic)
                                            :partition-count (env :input-topic-partitions)
                                            :replication-factor (env :input-topic-replication-factor)
                                            :key-serde (jsonserde/serde)
                                            :value-serde (jsonserde/serde)}
                                    :output {:topic-name (env :output-topic)
                                             :partition-count (env :output-topic-partitions)
                                             :replication-factor (env :output-topic-replication-factor)
                                             :key-serde (jsonserde/serde)
                                             :value-serde (jsonserde/serde)}}}]
    (log/info "Starting application with config:\n"
              (with-out-str
                (pprint app-config)))))
