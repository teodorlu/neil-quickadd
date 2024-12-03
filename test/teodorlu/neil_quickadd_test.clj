(ns teodorlu.neil-quickadd-test
  (:require [clojure.test :refer [deftest is testing]]
            [teodorlu.neil-quickadd :as neil-quickadd]))

(deftest safely-read-edn
  (testing "File exists"
    (is (map? (#'neil-quickadd/safely-slurp-edn "deps.edn" ::fallback))))
  (testing "File does not exist"
    (is (= ::fallback (#'neil-quickadd/safely-slurp-edn "does-not-exist.edn" ::fallback)))))
