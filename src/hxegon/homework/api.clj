(ns hxegon.homework.api
  (:require
    [ring.adapter.jetty :as jetty] ; see: jvm-opts in :run-m alias
    [reitit.ring :as ring]
    [muuntaja.core :as m]
    [reitit.ring.middleware.muuntaja :as m-middle]))

(defn get-records-handler [_]
  {:status 200
   :body "Not implemented"})

(defn not-found-handler [{:keys [uri]}]
  {:status 404
   :body (format "Could not find page at route %s" uri)})

(def app
  (ring/ring-handler
    (ring/router
      ["/records"
       ["" {:get get-records-handler}]]
      {:data {:muuntaja m/instance
              :middleware [m-middle/format-middleware]}})
    (ring/create-default-handler
      {:not-found not-found-handler})))

(defn main
  [_state]
  (println "starting api server...")
  (jetty/run-jetty app
                   {:port 3000
                    :join? false}))
