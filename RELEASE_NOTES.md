If you're upgrading to a new version of the SDK,
see this list for Java APIs that have changed:

## Release 1.0.82

The `login` methods in TokenIO and TokenIOAsync were renamed to `useMember`.

## Release 1.0.79

MemberAsync.aliases() and MemberAsync.firstAlias() now return
Observables instead of values. Member and MemberAsync used to
cache their aliases; now they fetch them from the cloud as needed.

## Release 1.0.64

getBalance methods for Member and Account have been
replaced by separate methods for getAvailableBalance
and getCurrentBalance.

## Release 1.0.60

The TokenIO.notifyPaymentRequest method parameters changed.
The method now takes one parameter, the TokenPayload.