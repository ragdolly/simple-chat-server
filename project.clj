(defproject chat "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.immutant/web "2.1.5"]
                 [ring/ring-core "1.5.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.2.1"]
                 [compojure "1.5.1"]
                 [environ "1.1.0"]
                 [cheshire "5.6.3"]]
  :main chat.core ;; TODO
  :uberjar-name "chat-standalone.jar" ;; TODO
  :profiles {:uberjar {:aot [chat.core]}} ;; TODO
  :min-lein-version "2.4.0")
