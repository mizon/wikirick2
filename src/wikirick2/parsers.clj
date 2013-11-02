(ns wikirick2.parsers
  (:use blancas.kern.core
        [hiccup.core :only [h]])
  (:require [blancas.kern.lexer.basic :as lexer]
            [clojure.string :as string]))

(defn scan-wiki-links [wiki-source]
  (set (map second (re-seq #"\[\[(.+?)\]\]" wiki-source))))

(def- special-chars " #")

(defn- debug [msg]
  (satisfy (fn [_]
             (prn msg)
             true)))

;; (do (prn content) (if content
;;                                 (>> single-line single-line)
;;                                 (fail "in underline-h1")))
(def wiki-parser
  (let [eol (<|> new-line* eof)

        trim-spaces (skip-many (<|> space tab))

        single-line (bind [ls (many-till any-char eol)]
                      (return (apply str ls)))

        not-special-char (not-followed-by (one-of* special-chars))

        underline-h1 (bind [_ not-special-char
                            content (look-ahead (<< single-line (many1 (sym* \=)) eol))
                            _ (if content
                                (do (prn "succ") (>> single-line single-line))
                                (do (prn "failed") (fail "in underline-h1")))
                            ]
                       (return [:h1 (h content)]))

        prefix-headline (bind [level (token* "######"
                                             "#####"
                                             "####"
                                             "###"
                                             "##"
                                             "#")
                               content single-line]
                          (>> trim-spaces
                              (return [(keyword (str "h" (count level))) (h content)])))

        headline underline-h1

        block-element headline]
    underline-h1))

(defn render-wiki-source [wiki-source]
  (let [result (parse wiki-parser wiki-source)]
    (if (:ok result)
      (:value result)
      result)))
