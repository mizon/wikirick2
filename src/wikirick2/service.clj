(ns wikirick2.service)

(defprotocol Service
  )

(defrecord WikiService [])

(extend-protocol Service
  )

(def ^:dynamic wiki-service (WikiService.))
