(ns chat.core
  (:require [immutant.web :as web]
            [immutant.web.async :as async]
            [environ.core :refer (env)]
            [cheshire.core :refer (generate-string parse-string)])
  (:gen-class))

;; Current Connections
;; {<connection1> {:connected-channel-id 1} ...}
(def conns (atom {}))

;; Current Channels

(def sample-channels {1 {:id 1
                         :name "channel-1"
                         :conns []
                         :messages [{:content "Hello From 1!" :username "john1"}]}
                      2 {:id 2
                         :name "channel-2"
                         :conns []
                         :messages [{:content "Howdy from 2" :username "john2"}]}})

(def channels (atom sample-channels))

;; Connection Management
(defn register-conn [conn]
  (swap! conns assoc conn {:connected-channel-id nil}))

(defn deregister-conn [conn]
  (swap! conns dissoc conn))

;; Channel Operations
(defn send-previous-chat-messages [conn]
  (let [connected-channel-id (get-in @conns [conn :connected-channel-id])
        chat-messages        (get-in @channels [connected-channel-id :messages])]
    (async/send! conn (generate-string {:event "channel-messages"
                                        :data  {:messages chat-messages}}))))

(defn publish-chat-message-to-channel [chat-message channel-id]
  (let [channel (@channels channel-id)
        participants (:conns channel)]
    (doseq [conn participants]
      (async/send! conn (generate-string {:event "new-message"
                                          :data chat-message})))))

(defn handle-join-channel [conn channel-id]
  (let [prev-channel-id (get-in @conns [conn :connected-channel-id])]
    (swap! channels update-in [prev-channel-id :conns]
           (fn [conns] (remove #(= % conn) conns)))
    (swap! channels update-in [channel-id :conns] #(conj % conn))
    (swap! conns assoc-in [conn :connected-channel-id] channel-id)
    (send-previous-chat-messages conn)))

(defn handle-add-message [conn chat-message]
  (let [channel-id (get-in @conns [conn :connected-channel-id])]
    (swap! channels update-in [channel-id :messages] #(conj % chat-message))
    (publish-chat-message-to-channel chat-message channel-id)))

(def websocket-callbacks
  (letfn [(handle-connection [conn]
            (let [data {:event "connected"
                        :data  {:channels (map #(dissoc % :conns :messages) (vals @channels))}}]
              (async/send! conn (generate-string data))))

          (handle-message [conn message]
            (let [message'             (parse-string message true)
                  {:keys [event data]} message']
              (condp = event
                "join-channel" (handle-join-channel conn (:id data))
                "add-message"  (handle-add-message conn data))))

          (handle-error [conn err]
            (println "error!"))

          (handle-on-close [conn {:keys [code reason]}]
            (deregister-conn conn)
            (println "closing connection due to code " code ", reason: "  reason))]
    {:on-open    handle-connection
     :on-message handle-message
     :on-error   handle-error
     :on-close   handle-on-close}))

(defn app [request]
  (async/as-channel request websocket-callbacks))

(defn -main [& {:as args}]
  (web/run
    app
    (merge {"host" (env :demo-web-host) "port" (env :demo-web-port)} args)))
