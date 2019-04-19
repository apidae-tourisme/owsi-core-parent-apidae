import invoke

from invoke import task, Collection
from invoke_sphinx import docs

ns = Collection(docs)

