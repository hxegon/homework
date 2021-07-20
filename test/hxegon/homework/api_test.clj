(ns hxegon.homework.api-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [hxegon.homework.api :as api]
   [muuntaja.core :as m]
   [clojure.pprint :as pp]
   [clojure.string :as string]
   [hxegon.homework.person :as p]))

;; Helpers

(defn get-records
  "Test helper function for GET /records.
  return value is response from api/app"
  []
  (api/app {:request-method :get
            :uri "/records"}))

(defn post-records
  "Test helper function for POSTing to /records endpoint.
  body is merged into a request as the value for :body-params
  response from api/app is returned"
  [body]
  (let [base {:request-method :post
              :uri "/records"
              :headers {"content-type" "application/json"}}
        request (merge base {:body-params body})]
    (api/app request)))

(defn sorted-records
  "Test helper function for GETting /records/<method> endpoints
  response value from api/app is returned"
  [method]
  (let [request {:request-method :get
                 :uri (str "/records/" method)}]
    (api/app request)))

(defn reset-people []
  (reset! api/people []))

(defmacro with-people
  "Runs the body against an api/people value set to ps, then pops back to the previous state of api/people
  THREADSAFETY IS NOT GUARANTEED
  Ex:
  ; starting with empty @people
  (with-people [(p/line->person :pipe \"Smith | John | Male | Blue | 01/01/2000\")]
    (is (= 1 (count (m/decode-response-body (get-records)))))
  ; @people is back to being empty"
  [ps & body]
  `(let [people-bak# @api/people]
     (try
       (do (reset! api/people ~ps)
           ~@body)
       (finally (reset! api/people people-bak#)))))

;; Helper tests

(deftest with-people-helper-test
  (testing "saves people's state, changes it to given argument and sets it back after"
    (reset-people)
    (api/add-person (p/person ["Foo" "Jake" "Male" "Red" "01/02/2001"]))
    (with-people [(p/line->person :pipe "Smith | John | Male | Blue | 01/01/2000")]
      (is (= "John" (->> (get-records) m/decode-response-body first :firstname))))
    (is (= "Jake" (->> (get-records) m/decode-response-body first :firstname)))
    (reset-people)))

;; API tests

(deftest routing-sanity-test
  (testing "200 response for"
    (testing "GET /records"
      (let [request {:request-method :get
                     :uri "/records"}
            response (api/app request)]
        (is (= 200 (:status response))))))
  (testing "404 response for GET /non-existent-route"
    (let [request {:request-method :get
                   :uri "/non-existant-route"}
          response (api/app request)]
      (is (= 404 (:status response))))))

(deftest records-test
  (testing "GET /records starts with no people"
    (let [body (m/decode-response-body (get-records))]
      (is (= [] body))))
  (testing "POST /records returns 400 without required params keys"
    (is (= 400 (:status (post-records {:delimiter "pipe"}))))
    (is (= 400 (:status (post-records {:data "foo"})))))
  (testing "POST /records returns 422 bad param values with invalid delimiter"
    (is (= 422 (:status (post-records {:delimiter "invalid" :data "not evaluated"})))))
  (testing "POST /records with good person data"
    (let [post-resp (post-records {:delimiter "pipe"
                                   :data "Smith | John | Male | Blue | 01/01/2001"})
          get-resp (get-records)
          records (m/decode-response-body get-resp)]
      (testing "is successful"
        (is (= 200 (:status post-resp))))
      (testing "get /records is successful"
        (is (= 200 (:status get-resp))))
      (testing "is reflected in GET /records"
        (is (= "John" (get (first records) :firstname)))))
    (reset-people))
  (testing "GET /records displays a birthdate as M/D/YYYY"
    (with-people [(p/person ["Smith" "John" "Male" "Blue" "01/01/2001"])]
      (let [resp (get-records)
            first-person (first (m/decode-response-body resp))]
        (is (= 200 (:status resp)))
        (is (= "1/1/2001" (:dob first-person)))))))

(def test-people
  (map p/person
       [["Smith" "John" "Male" "Blue" "01/01/2000"]
        ["Doe" "Jane" "Female" "Red" "01/02/2000"]
        ["Zed" "Jake" "Male" "Green" "01/01/1999"]]))

(deftest sorted-records-test
  (with-people test-people
    (testing "/records/gender sorts people by gender, female, then male and then by last name"
      (let [resp (sorted-records "gender")]
        (is (= 200 (:status resp)))
        (is (= ["Jane" "John" "Jake"]
               (map :firstname (m/decode-response-body resp))))))
    (testing "/records/name returns records sorted by firstname lastname (ascending)"
      (let [resp (sorted-records "name")]
        (is (= 200 (:status resp)))
        (is (= ["Jake" "Jane" "John"]
               (map :firstname (m/decode-response-body resp))))))
    (testing "/records/birthdate returns records sorted by birthdate (ascending)"
      (let [resp (sorted-records "birthdate")]
        (is (= 200 (:status resp)))
        (is (= ["Jake" "John" "Jane"]
               (map :firstname (m/decode-response-body resp))))))))
