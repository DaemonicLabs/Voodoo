# TODO

## Builder

* re-add a validate step

* fix cursedata parsing slowdown

* clean up unneeded duplication between features classes

* entry deduplication (2 entries with the same name cause endless looping)

**TEST** manual dependencies with optionals





* use property wrappers Prop<T>(var enabled: boolean, var value: T)
  * required for planned advanced gui features


* allow nested lists of entries
  * flatten tree into list
  + requires processing entries without modification in-place