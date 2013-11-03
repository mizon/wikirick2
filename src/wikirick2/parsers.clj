(ns wikirick2.parsers
  (:use [hiccup.core :only [h]]
        zetta.core)
  (:require [clojure.string :as string]
            [zetta.combinators :as c]
            [zetta.parser.seq :as s]))

(declare wiki)

(defn scan-wiki-links [wiki-source]
  (set (map second (re-seq #"\[\[(.+?)\]\]" wiki-source))))

(defn render-wiki-source [wiki-source]
  (let [source (string/split-lines wiki-source)
        result (parse-once wiki source)
        value (:result result)]
    (if value
      value
      result)))

(defmacro def- [& forms]
  `(def ^:private ~@forms))

(defn- match? [reg]
  (s/satisfy? #(re-matches reg %)))

(defn- try-parser [parser]
  (fn [input more err-fn ok-fn]
    (letfn [(err-fn0 [_ more0 stack msg]
              (err-fn input more0 stack msg))]
      (parser input more err-fn0 ok-fn))))

(def- plain-line
  (match? #"[^#>\s].*"))

(def- empty-line
  (match? #"\s*"))

(def- headline-prefix
  (let [regex #"(#+) *(.*?) *#*"]
    (do-parser [line (match? regex)]
      (let [[_ syms content] (re-matches regex line)]
        [(keyword (str "h" (count syms))) content]))))

(def- headline-underline
  (try-parser (do-parser [content s/any-token
                          underline (match? #"(=+|-+) *")]
                (if (= (first underline) \=)
                  [:h1 content]
                  [:h2 content]))))

(def- paragraph
  (do-parser [lines (c/many1 plain-line)]
    [:p (string/join " " lines)]))

(def- block
  (reduce <|> [headline-prefix
               headline-underline
               paragraph]))

(def- wiki
  (do-parser [bs (c/many (*> (c/many empty-line) block))
              _ (c/many empty-line)
              _ s/end-of-input]
    bs))
