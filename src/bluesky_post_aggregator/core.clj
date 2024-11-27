(ns bluesky-post-aggregator.core
  (:require [environ.core :refer [env]]
            [jackdaw.serdes.json :as jsonserde]
            [jackdaw.streams :as js]
            [clojure.tools.logging :as log]
            [bluesky-post-aggregator.topology :refer [topology-builder]]
            [clojure.pprint :refer [pprint]])
  (:gen-class))

(defn -main
  [& args]
  (let [app-config {"bootstrap.servers" "0.0.0.0:29092"
                    "application.id" "test"
                    "auto.offset.reset" "earliest"
                    "cache.max.bytes.buffering" "100000"
                    "num.stream.threads" 8
                    "commit.interval.ms" 1000
                    "topic-config" {:input {:topic-name "bluesky_input_stream"
                                            :partition-count 8
                                            :replication-factor 1
                                            :key-serde (jsonserde/serde)
                                            :value-serde (jsonserde/serde)}
                                    :output {:topic-name "bluesky_aggregator_output"
                                             :partition-count 8
                                             :replication-factor 1
                                             :key-serde (jsonserde/serde)
                                             :value-serde (jsonserde/serde)}}}
        streams-builder (js/streams-builder)
        topology (topology-builder streams-builder app-config)
        app (js/kafka-streams topology app-config)]
    (log/info "Starting application with config:\n"
              (with-out-str
                (pprint app-config)))
    (js/start app)
    app))
