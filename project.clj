(defproject bluesky-post-aggregator "0.1.0-SNAPSHOT"
  :description "A Kafka Streams Application"
  :url "https://github.com/fergalmoylan/bluesky-post-aggregator"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [environ "1.2.0"]
                 [fundingcircle/jackdaw "0.9.12"]
                 [org.clojure/tools.logging "1.3.0"]
                 [ch.qos.logback/logback-classic "1.4.11"]]
  :main ^:skip-aot bluesky-post-aggregator.core
  :repl-options {:init-ns bluesky-post-aggregator.core}
  :target-path "target/%s"
  :resource-paths ["resources"]
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
