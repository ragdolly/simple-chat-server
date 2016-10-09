(ns chat.core
  (:require [immutant.web :as web]
            [immutant.web.async :as async]
            [environ.core :refer (env)]
            [cheshire.core :refer (generate-string parse-string)])
  (:gen-class))

;; TODO
;; use middleware for parsing and encoding json strings

;; globals
;; keep track of all messages
;; and all existing channels

(def messages (atom [{:content "hi" :username "erikay"}]))
(def channels (atom []))

(defn register-channel [ch]
  (swap! channels conj ch))

(defn deregister-channel [ch]
  (swap! channels (fn [channels] (remove #(= ch %) channels))))

(defn store-message [msg]
  (swap! messages conj msg))

(defn send-previous-msgs [ch]
  (async/send! ch (generate-string @messages)))

(defn broadcast-message [msg]
  (doseq [ch @channels]
    (async/send! ch (generate-string msg))))

(def websocket-callbacks
  {:on-open (fn [ch]
              (register-channel ch)
              (send-previous-msgs ch))
   :on-message (fn [ch message]
                 (println "received " message)
                 (let [msg (parse-string message)]
                   (store-message msg)
                   (broadcast-message msg)))
   :on-error (fn [ch err]
               (println "error!"))
   :on-close (fn [ch {:keys [code reason]}]
               (deregister-channel ch)
               (println "closing connection!"))})

(defn app [request]
  (println request)
  (async/as-channel request websocket-callbacks))

(defn -main [& {:as args}]
  (web/run
    app
    (merge {"host" (env :demo-web-host) "port" (env :demo-web-port)} args)))
