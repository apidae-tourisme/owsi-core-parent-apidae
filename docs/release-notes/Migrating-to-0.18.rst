.. _migration_0.18:

Migrating to 0.18
=================

This guide aims at helping OWSI-Core users migrate an application based on OWSI-Core 0.18 to OWSI-Core 0.17.

In order to migrate from an older version of OWSI-Core, please refer to :ref:`migration_0.17` first.

Breaking changes
----------------

Satrting from the 0.18 version, the owsi-core project is now hosted on Smile tools (gitlab, jenkins, ...).

The most important change is: the `groupId` is now ``fr.smile.core.xxx`` instead of ``fr.openwide.core.xxx``.

For security reasons, Spring has been updated from `4.3.8.RELEASE` to `4.3.19.RELEASE`.


New features
------------

* The war is **not** deployed on the test server anymore during the ``mvn deploy`` phase. It was not relevant for ht e`test` profile anyway (the one used to make official releases).
* Some code has been updated to allow Eclipse compilation for Eclipse >= 4.7

