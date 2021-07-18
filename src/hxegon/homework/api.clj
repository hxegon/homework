(ns hxegon.homework.api
  (:require
   [ring.adapter.jetty :as jetty] ; see: jvm-opts in :run-m alias
   [reitit.ring :as ring]
   [muuntaja.core :as m]
   [reitit.ring.middleware.muuntaja :as m-middle]
   [hxegon.homework.internal :refer [record->map key-of-m?]]
   [hxegon.homework.person :as p]
   [clojure.string :as string]))

(def people (atom []))

(defn add-person
  "Add a person. Wraps the semantics of atom operations. Threadsafe."
  [person]
  (swap! people conj person))

(defn get-records-handler [_]
  {:status 200
   :body (into [] (map record->map) @people)})

(defn post-records-handler [{{:keys [delimiter data]} :body-params}]
  (if (or (nil? delimiter) (nil? data))
    {:status 400
     :body "An expected key (:delimiter or :data) was missing from the body."}
    (let [{:keys [error-msg] :as result} (p/line->person (keyword delimiter) data)]
      (if error-msg
        {:status 422
         :body (format "Couldn't parse data into person.\n%s" error-msg)}
        (do (add-person result)
            {:status 200
             :body "Person added successfully."})))))

(defn not-found-handler [{:keys [uri]}]
  {:status 404
   :body (format "Could not find page at route %s" uri)})

(def app
  (ring/ring-handler
    (ring/router
      ["/records"
       ["" {:get get-records-handler
            :post post-records-handler}]]
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
