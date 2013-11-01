(ns wikirick2.parsers
  (:use blancas.kern.core
        blancas.kern.lexer.basic
        [hiccup.core :only [h]])
  (:require [clojure.string :as string]))

(defn scan-wiki-links [wiki-source]
  (set (map second (re-seq #"\[\[(.+?)\]\]" wiki-source))))

(def- wiki-parser
  (let [trim-spaces (skip-many (<|> space tab))

        single-line (bind [ls (many-till any-char (<|> new-line eof))]
                      (return (apply str ls)))

        headline (bind [level (<|> (>> (token "######") (return 6))
                                   (>> (token "#####") (return 5))
                                   (>> (token "####") (return 4))
                                   (>> (token "###") (return 3))
                                   (>> (token "##") (return 2))
                                   (>> (token "#") (return 1)))
                        content single-line]
                   (>> trim-spaces
                       (return [(keyword (str "h" level)) (h content)])))

        block-element headline]
    (many block-element)))

(defn render-wiki-source [wiki-source]
  (let [result (parse block-element wiki-source)]
    (if (:ok result)
      (:value result)
      :error)))
