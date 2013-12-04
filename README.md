Wikirick2
=========
A RCS-based wiki engine for personal sites. This software is in development and beta quality yet.

Features
--------
* Implemented in [Clojure][]
* RCS-based
* Keeping revision history and able to see them
* Adopting the Markdown syntax
* Linking automatically between any related pages

Requirments
-----------
* Clojure 1.5+
* [Leiningen][] 2.x
* Linux (I will support other platforms in the future)

Installation
------------
Before installation, you must get Leiningen which is a package manager for Clojure programming language.

After installed Leiningen, clone this repository and type the next command in the direcotry please:

    $ lein ring server

Wikirick2 will start serving, and you can see the site on `http://localhost:3000/` .

[Clojure]: http://clojure.org/
[Leiningen]: http://leiningen.org/
