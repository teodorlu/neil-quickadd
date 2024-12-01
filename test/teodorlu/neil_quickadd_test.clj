(ns teodorlu.neil-quickadd-test
  (:require [clojure.test :refer [deftest is]]
            [teodorlu.neil-quickadd :as neil-quickadd]))

(deftest safely-read-edn
  (is (map? (#'neil-quickadd/safely-slurp-edn "deps.edn" ::fallback))))
