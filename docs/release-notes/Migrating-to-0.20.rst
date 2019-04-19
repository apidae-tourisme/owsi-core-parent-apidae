.. _migration_0.20:

Migrating to 0.20
=================

This guide aims at helping OWSI-Core users migrate an application based on OWSI-Core 0.19 to OWSI-Core 0.20.

In order to migrate from an older version of OWSI-Core, please refer to :ref:`migration_0.19` first.

Fixes
-----

This version fixes a potential NPE in *infinispan*.

New features
------------

* Make `QueuedTaskHolder` ``stop()`` *transactional*

