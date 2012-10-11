(defproject log "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-histogram "0.0.1-SNAPSHOT"]
                 [clj-time "0.4.4"]
                 [org.clojure/tools.nrepl "0.2.0-SNAPSHOT"] ;; "0.2.0-beta9"]
                 [protobuf "0.6.1"]]
  :plugins [[lein-protobuf "0.2.1"]]
  :main log.core)
