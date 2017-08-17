(ns commiteth.eth.multisig-wallet
  (:require [commiteth.eth.core :as eth]
            [clojure.tools.logging :as log]))

(defonce confirmation-topic "0xc0ba8fe4b176c1714197d43b9cc6bcf797a4a7461c5fe8d0ef6e184ae7601e51")

(defonce method-ids
  {:submit-transaction (eth/sig->method-id "submitTransaction(address,uint256,bytes)")
   :withdraw-everything (eth/sig->method-id "withdrawEverything(address)")})


(defn execute
  [contract to value]
  (log/debug "multisig.execute(contract, to, value)" contract to value)
  (eth/execute (eth/eth-account)
               contract
               (:submit-transaction method-ids)
               to
               value
               "0x60"
               "0x0"
               "0x0"))

(defn find-confirmation-hash
  [receipt]
  (let [logs                   (:logs receipt)
        has-confirmation-event #(some (fn [topic] (= topic
                                                    confirmation-topic))
                                      (:topics %))
        confirmation-event     (first (filter has-confirmation-event logs))
        confirmation-data      (:data confirmation-event)]
    (when confirmation-data
      (subs confirmation-data 2 66))))

(defn send-all
  [contract to]
  (log/debug "multisig.send-all(contract, to)" contract to)
  (let [params (eth/format-call-params
                (:withdraw-everything method-ids)
                to)]
    (eth/execute (eth/eth-account)
                 contract
                 (:submit-transaction method-ids)
                 contract
                 0
                 "0x60"
                 "0x24"
                 params)))
