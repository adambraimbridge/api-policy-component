Policy Component
================

An HTTP service provides a facade over the reader endpoint for use by licenced partners.

* adds calculated fields for use by B2B partners
* blocks or hides content that is not permitted to the partner
* rewrites queries according to account configuration

INTERFACE
=========

This facade deliberately does not define it's own set of endpoints or interface contracts
instead it makes specific modifications to the interface of the Reader API and has
minimal knowledge of them.

