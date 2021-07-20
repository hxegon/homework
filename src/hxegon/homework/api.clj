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

(defn encode-people
  "Prep a collection of people for use in a response."
  [people]
  (into []
        (comp (map #(into {} %))
              (map #(update % :dob p/render-dob)))
        people))

(defn get-records-handler [_]
  {:status 200
   :body (encode-people @people)})

(defn post-records-handler [{{:keys [delimiter data]} :body-params}]
  (if (or (nil? delimiter) (nil? data))
    {:status 400
     :body "<h1>An expected key (:delimiter or :data) was missing from the body</h1>"}
    (let [{:keys [error-msg] :as result} (p/line->person (keyword delimiter) data)]
      (if error-msg
        {:status 422
         :body (format "<h1>Couldn't parse data into person</h1>\n%s" error-msg)}
        (do (add-person result)
            {:status 200
             :body "<h1>Person added successfully</h1>"})))))

(defn mk-sorted-records-handler
  [sorter]
  (fn sorted-records-handler [_]
    {:status 200
     :body (encode-people (sorter @people))}))

(defn not-found-handler [{:keys [uri]}]
  {:status 404
   :body (format "Could not find page at route %s" uri)})

(def app
  (ring/ring-handler
   (ring/router
    ["/records"
     ["" {:get get-records-handler
          :post post-records-handler}]
     ["/gender" {:get (mk-sorted-records-handler (:gender p/people-sorters))}]
     ["/name" {:get (mk-sorted-records-handler (:name p/people-sorters))}]
     ["/birthdate" {:get (mk-sorted-records-handler (:birthdate p/people-sorters))}]]
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
