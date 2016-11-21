(ns chat.commands
  (:require [immutant.web.async :as async]))
;;
;; conns = {<connection> {:username ... :connected-channel-id ...}}
;; user->conn = {<username>: <connection>}
;;
;;
;;(def sample-channels {1 {:id 1
;;                         :name "channel-1"
;;                         :conns []
;;                         :messages [{:content "Hello From 1!" :username "john1"}]}
;;                      2 {:id 2
;;                         :name "channel-2"
;;                         :conns []
;;                         :messages [{:content "Howdy from 2" :username "john2"}]}})
;;

(def conns (atom {}))
(def user->conn (atom {}))
(def channels (atom {}))

;; User / Connection Management
(defn sign-in [conn username]
  (swap! conns assoc conn {:connected-channel-id nil
                           :username username})
  (swap! user->conn assoc username conn))

(defn sign-out [conn]
  (swap! user->conn dissoc (-> conn conns :username))
  (swap! conns dissoc conn))

;; Channel Operations
(defn get-channels [username]
  {:channels (map #(select-keys % [:id :name]) (values @channels))})

(defn get-users-in-a-channel [channel-id]
  (let [conns'           (@conns)
        conns-in-channel (-> (@channels channel-id) :conns)
        conn->user       (fn [conn] (-> (conns' conn) :username))])
  {:users (map conn->user conns-in-channel)})

(defn join-channel [username channel-id]
  (let [conn (user->conn username)
        prev-channel-id (get-in @conns [conn :connected-channel-id])]
    (swap! channels update-in [prev-channel-id :conns]
           (fn [conns] (remove #(= % conn) conns)))
    (swap! channels update-in [channel-id :conns] #(conj % conn))
    (swap! conns assoc-in [conn :connected-channel-id] channel-id)
    (send-previous-messages conn)))

(defn send-previous-messages [conn]
  (let [connected-channel-id (get-in @conns [conn :connected-channel-id])
        messages        (get-in @channels [connected-channel-id :messages])]
    (async/send! conn (generate-string {:event "messages"
                                        :data  {:messages messages}}))))

(defn publish-message-to-channel [message channel-id]
  (let [channel (@channels channel-id)
        participants (:conns channel)]
    (doseq [conn participants]
      (async/send! conn (generate-string {:event "send-new-message"
                                          :data message})))))

(defn send-new-message [conn message]
  (let [username (-> conn conns :username)
        channel-id (get-in @conns [conn :connected-channel-id])
        message' (assoc message :username username)]
    (swap! channels update-in [channel-id :messages] #(conj % message'))
    (publish-message-to-channel message' channel-id)))
