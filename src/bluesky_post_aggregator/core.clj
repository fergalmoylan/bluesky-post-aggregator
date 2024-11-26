(ns bluesky-post-aggregator.core
  (:require [environ.core :refer [env]]
            [jackdaw.serdes.json :as jsonserde]
            [jackdaw.streams :as js]
            [clojure.tools.logging :as log]
            [bluesky-post-aggregator.topology :refer [topology-builder]]
            [clojure.pprint :refer [pprint]]))

(defn -main
  [& args]
  (let [app-config {"bootstrap.servers" (env :kafka-addresses)
                    "application.id" (env :application-id)
                    "auto.offset.reset" "earliest"
                    "cache.max.bytes.buffering" "100000"
                    "topic-config" {:input {:topic-name (env :input-topic)
                                            :partition-count (env :input-topic-partitions)
                                            :replication-factor 1
                                            :key-serde (jsonserde/serde)
                                            :value-serde (jsonserde/serde)}
                                    :output {:topic-name (env :output-topic)
                                             :partition-count (env :output-topic-partitions)
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
