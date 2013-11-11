(ns wikirick2.parsers
  (:use [hiccup.core :only [h]]
        zetta.core)
  (:require [clojure.string :as string]
            [zetta.combinators :as c]
            [zetta.parser.seq :as s]))

(declare wiki li-level)

(defn scan-wiki-links [wiki-source]
  (set (map second (re-seq #"\[\[(.+?)\]\]" wiki-source))))

(defn render-wiki-source [wiki-source]
  (let [source (string/split-lines wiki-source)
        result (parse-once wiki source)
        value (:result result)]
    (if value
      value
      result)))

(defn- unlines [lines]
  (string/join "\n" lines))

(defmacro def- [name value]
  (list `def (with-meta name {:private true}) value))

(defn- match-line [regex]
  (fn [input more err-fn ok-fn]
    (let [l (first input)
          matched (and l (re-matches regex l))]
      (if matched
        (ok-fn (rest input) more matched)
        (err-fn input more [] "match-line")))))

(defn- try-parser [parser]
  (fn [input more err-fn ok-fn]
    (letfn [(err-fn* [_ more* stack msg]
              (err-fn input more* stack msg))]
      (parser input more err-fn* ok-fn))))

(defn- not-followed-by [parser]
  (fn [input more err-fn ok-fn]
    (letfn [(ok-fn* [_ more* result]
              (err-fn input more* [] "not followed by"))

            (err-fn* [_ more* _ _]
              (s/any-token input more* err-fn ok-fn))]
      (parser input more err-fn* ok-fn*))))

(def- empty-line
  (match-line #"\s*"))

(def- atx-header
  (do-parser [[_ syms content] (match-line #"(#{1,6}) *(.*?) *#*")]
    [(keyword (str "h" (count syms))) content]))

(def- settext-header
  (try-parser (do-parser [content s/any-token
                          [_ underline] (match-line #"(=+|-+) *")]
                (case (first underline)
                  \= [:h1 content]
                  \- [:h2 content]
                  (assert false "must not happen")))))

(declare ul-item-cont)

(defn- ul-item-cont-lines [level]
  (let [indented (try-parser (do-parser [es (c/many empty-line)
                                         [_ content] (match-line #" {4}(.+)")]
                               (if (empty? es)
                                 content
                                 ["" content])))
        no-indented (not-followed-by (<|> (ul-item-cont level) empty-line))]
    (c/many (<|> indented no-indented))))

(def- ul-item
  (let [start (do-parser [[_ spaces content] (match-line #"( {0,3})[\*\+\-]\s+(.*)")]
                [(count spaces) content])]
    (do-parser [[level l] start
                ls (ul-item-cont-lines level)
                blanks (c/many empty-line)]
      [level (flatten (if (empty? blanks)
                        (cons l ls)
                        (list* "" l ls)))])))

(defn- ul-item-cont [level]
  (do-parser [[_ l] (match-line (re-pattern (format " {%s}[\\*\\+\\-]\\s+(.*)" level)))
              ls (ul-item-cont-lines level)
              blanks (c/many empty-line)]
    (flatten (if (empty? blanks)
               (cons l ls)
               (list* "" l ls)))))

(declare unordered-list)

(def- ul-plain-lines
  (do-parser [lines (c/many1 (not-followed-by unordered-list))]
    (unlines (map #(.trim %) lines))))

(def- unordered-list
  (letfn [(plain-mode [liness]
            (for [lines liness]
              `[:li ~@(:result (parse-once (c/many (<|> unordered-list ul-plain-lines)) lines))]))

          (paragraph-mode [liness]
            (for [lines liness]
              `[:li ~@(:result (parse-once wiki lines))]))]
    (do-parser [[level ls] ul-item
                lss (c/many (ul-item-cont level))
                :let [liness (cons ls lss)]]
      `[:ul ~@(if (empty? (filter #(= % "") (flatten liness)))
                (plain-mode liness)
                (paragraph-mode liness))])))

(def- code
  (do-parser [:let [code-line (<$> first (match-line #"(\t|    )(.+)"))]
              l code-line
              ls (c/many (<|> code-line empty-line))
              :let [code-lines (cons l ls)
                    trim-left #(.replaceAll % "^(\t|    )" "")
                    trim-right #(.replaceAll % "\\s*$" "")]]
    [:pre [:code (trim-right (unlines (map trim-left code-lines)))]]))

(def- bq-marked-line
  (<$> second (match-line #"\s*> ?(.*)")))

(def- bq-no-marked-line
  (not-followed-by empty-line))

(def- bq-fragment
  (do-parser [l bq-marked-line
              ls (c/many (<|> bq-marked-line bq-no-marked-line))
              _ (c/skip-many empty-line)]
    `(~l ~@ls "")))

(def- blockquote
  (do-parser [fragments (c/many1 bq-fragment)
              :let [inners (parse-once wiki (apply concat fragments))]]
    (if (:result inners)
      `[:blockquote ~@(:result inners)]
      (assert false "blockquote: must not happen"))))

(def- paragraph
  (do-parser [ls (c/many1 (not-followed-by (reduce <|> [unordered-list
                                                        code
                                                        atx-header
                                                        settext-header
                                                        blockquote
                                                        empty-line])))]
    [:p (unlines (map #(.trim %) ls))]))

(def- block
  (reduce <|> [unordered-list
               code
               atx-header
               settext-header
               blockquote
               paragraph]))

(def- wiki
  (do-parser [bs (c/many (*> (c/many empty-line) block))
              _ (c/many empty-line)
              _ s/end-of-input]
    bs))
