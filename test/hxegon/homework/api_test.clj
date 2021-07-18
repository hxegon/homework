(ns hxegon.homework.api-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [hxegon.homework.api :as api]))


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
