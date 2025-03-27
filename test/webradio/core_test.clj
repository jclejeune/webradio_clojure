(ns webradio.core-test
  (:require [clojure.test :as t]
            [webradio.model :as model]
            [webradio.ui :as ui]))

(t/deftest initial-state-test
  (t/testing "Initial radio list is not empty"
    (t/is (seq @model/radios)))

  (t/testing "Current player starts as nil"
    (t/is (nil? @model/current-player))))

(t/deftest ui-creation-test
  (t/testing "UI creation does not throw exceptions"
    (t/is (some? (ui/create-ui)))))