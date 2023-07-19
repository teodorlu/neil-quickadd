# Nezz: Neil with fuzz

Nezz (`nezz`, Neil with fuzz) is a fuzzy interface for managing Clojure
`deps.edn` files built on top of [Neil].

## Nezz was previously known as `neil-quickadd`

The Nezz command line interface is _not_ backwards compaitible with Neil-quickadd.
I consider Nezz a pure improvement over Neil-quckadd.

The last version of `neil-quckadd` can be installed with:

   bbin install io.github.teodorlu/neil-quickadd --sha 2589b5018ae1d29da75bd546622b1767d2dbb84d --as neil-quickadd

## Usage

`nezz scan DIR` - Scan `DIR` for `deps.edn` files with dependencies.

`nezz add` - Quickly add an indexed dependency.

## FAQ

**Q:** Is Nezz affiliated with [Neil]?
<br>
**A:**
Nezz is developed independent from Neil, and is not endorsed by Neil.

**Q:** Why should I use Nezz instead of Nezz?
<br>
**A:** I recommend using Nezz _in addition to_ Neil.
The main selling point for Nezz is a fine interface for adding dependencies without having to remember dependency coordinates.

## Installing Nezz

Nezz has some dependneices.
Please install them first:

0. Install [Babashka]
1. Install [Neil]
2. Install [fzf]
3. Install [bbin]

[Babashka]: https://github.com/babashka/babashka
[Neil]: https://github.com/babashka/neil
[fzf]: https://github.com/junegunn/fzf
[bbin]: https://github.com/babashka/bbin

Then install Nezz with [bbin]:

   ```
   bbin install io.github.teodorlu/nezz --latest-sha
   ```


## Installing a local development version

Clone the repo,

    git clone https://github.com/teodorlu/nezz.git
    cd nezz
        
then install the script with [bbin].

    bbin install . --as nezz-dev

## Doom Emacs usage

```emacs-lisp
;; TODO migrate elisp function names
;; packages.el
(package! neil-quickadd :recipe (:host github :repo "teodorlu/nezz" :files ("*.el")))

;; config.el
(use-package! neil-quickadd)
```

## Thanks!

Without [@borkdude][borkdude] and [@rads][rads], `nezz` wouldn't exist. Thank you!

[borkdude]: https://github.com/borkdude/
[rads]: https://github.com/rads/
