.PHONY: lint

# This transforms a list of dirs to dir1:dir2:dir3
classpathify = $(subst $(eval) ,:,$(wildcard $1))

# List all src dirs
DIRS := ${dir ${wildcard modules/*/src/}}

lint:
	clj-kondo --lint $(call classpathify,$(DIRS)) --config '{:output {:pattern "::{{level}} file={{filename}},line={{row}},col={{col}}::{{message}}"}}'
