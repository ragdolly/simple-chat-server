(ns chat.core
  (:require [immutant.web :as web]
            [immutant.web.async :as async]
            [environ.core :refer (env)]
            [cheshire.core :refer (generate-string parse-string)]
            [compojure.core :refer :all]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.json :refer (wrap-json-body wrap-json-response)]
            [chat.commands :as commands])
  (:gen-class))

(def websocket-callbacks
  {:on-open (fn [conn]
              (let [data {:event "connected" :data nil}]
                (async/send! conn (generate-string data))))

   :on-message (fn [conn message]
                 (let [message'             (parse-string message true)
                       {:keys [event data]} message']
                   (condp = event
                     "sign-in"
                     (commands/sign-in conn (:username data))

                     "new-message"
                     (commands/new-message conn data))))

   :on-error (fn [conn err] (println "error!"))
   :on-close (fn [conn {:keys [code reason]}]
               (commands/sign-out conn)
               (println "closing connection due to code " code ", reason: "  reason))})

;; TODO handle authentication and authorization later.
(def api-routes
  (routes
   (GET "/" request (async/as-channel request websocket-callbacks)) ;;
   (GET "/channels" [] (response get-channels)) ;; TODO
   (POST "/join" {{:keys [username channel-id]} :body}
         (commands/join-channel username channel-id))))

(defn wrap-api [routes]
  (-> routes
      wrap-json-response
      (wrap-json-body {:keywords? true})
      (wrap-defaults api-defaults)))

(defn -main [& {:as args}]
  (web/run
    (wrap-api api-routes)
    (merge {"host" (env :demo-web-host) "port" (env :demo-web-port)} args)))
