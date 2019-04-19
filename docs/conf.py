# Configuration file for the Sphinx documentation builder.
#
# This file only contains a selection of the most common options. For a full
# list see the documentation:
# http://www.sphinx-doc.org/en/master/config

# -- Path setup --------------------------------------------------------------

# If extensions (or modules to document with autodoc) are in another directory,
# add these directories to sys.path here. If the directory is relative to the
# documentation root, use os.path.abspath to make it absolute, like shown here.
#
# import os
# import sys
# sys.path.insert(0, os.path.abspath('.'))


# -- Project information -----------------------------------------------------

import yaml

with open("../invoke.yml", 'r') as stream:
  vars = yaml.safe_load(stream)

project = vars['project']
copyright = vars['copyright']
author = vars['author']

# The short X.Y version
version = vars['version']
# The full version, including alpha/beta/rc tags
release = vars['version']

# -- General configuration ---------------------------------------------------

# Add any Sphinx extension module names here, as strings. They can be
# extensions coming with Sphinx (named 'sphinx.ext.*') or your custom
# ones.
extensions = [
   'sphinx.ext.todo',
   'sphinx.ext.extlinks',
]

# Add any paths that contain templates here, relative to this directory.
templates_path = ['_templates']

# List of patterns, relative to source directory, that match files and
# directories to ignore when looking for source files.
# This pattern also affects html_static_path and html_extra_path.
exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store']


# -- Options for HTML output -------------------------------------------------

# The theme to use for HTML and HTML Help pages.  See the documentation for
# a list of builtin themes.
#
html_theme = 'sphinx_rtd_theme'

# Add any paths that contain custom static files (such as style sheets) here,
# relative to this directory. They are copied after the builtin static files,
# so a file named "default.css" will overwrite the builtin "default.css".
html_static_path = ['_static']

# -- Options for todo extension ----------------------------------------------
# If true, `todo` and `todoList` produce output, else they produce nothing.
todo_include_todos = True

# -- sphinx-versions configuration --------------------------------
extlinks = {
    'redmine':    ('https://redmine-projets.smile.fr/issues/%s', 'Issue '),
    'wiki':       ('https://wiki.smile.fr/view/%s',              ''),
    'git':        ('https://git.smile.fr/%s',                    ''),
    'doc-server': ('http://pic-java.vitry.intranet/%s',          ''),
}

# -- sphinx-versions configuration --------------------------------

scv_show_banner = True
scv_banner_greatest_tag = True

