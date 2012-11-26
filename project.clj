(defproject log "0.2.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-histogram "0.3.0-SNAPSHOT"]
                 [clj-time "0.4.4"]
                 [org.clojure/tools.nrepl "0.2.0-RC1"]
                 [midje "1.4.0"]
                 [org.flatland/protobuf "0.7.1"]]
  :plugins [[lein-protobuf "0.3.1"]
            [lein-midje "2.0.1"]]
  :jvm-opts ["-Xmx1g" "-server"]
  :main log.core)
