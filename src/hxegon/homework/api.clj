(ns hxegon.homework.api
  (:require
   [ring.adapter.jetty :as jetty] ; see: jvm-opts in :run-m alias
   [reitit.ring :as ring]
   [muuntaja.core :as m]
   [reitit.ring.middleware.muuntaja :as m-middle]
   [hxegon.homework.person :as p]
   [clojure.string :as string]))

(def people (atom []))

(defn add-person
  "Add a person to the people atom"
  [person]
  (swap! people conj person))

(defn encode-people
  "takes a coll of people records and encodes them into a representation more
  suitable for use in a response body"
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
     :body "An expected key (:delimiter or :data) was missing from the body"}
    (let [{:keys [error-msg] :as result} (p/line->person (keyword delimiter) data)]
      (if error-msg
        {:status 422
         :body (format "Data was not parsable as a person:\n%s" error-msg)}
        (do (add-person result)
            {:status 200
             :body "Person added successfully"})))))

(defn mk-sorted-records-handler
  "Takes a fn that sorts people and returns a request handler fn that contains
  the contents of people sorted by that fn"
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
  "Prints starting message and runs server on port 3000"
  [_state]
  (println "starting api server...")
  (jetty/run-jetty app
                   {:port 3000
                    :join? false}))
