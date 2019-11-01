# *Java SDK Release Notes*

If you're upgrading to the latest version of the SDK, here's what's new.

> ## Notice of Compulsory eIDAS Validation
>
> Effective 24 Spetember 2019, Token is required under PSD2 rules to validate third-party providers (TPPs) and their service requests on behalf of its PSD2-enabled banks. An initial validation check is performed during TTP onboarding to ensure that the TPP is in compliance with respective host-nation business licensing requirements for the TPP's planned service geography and that the TPP is using a legitimate eIDAS certificate issued by a qualified trust service provider (QTSP). Successful validation is required before Token can promote a TPP to production. Thereafter, automated validation checks are performed no less than 4 times per business day (GMT 0700-1800). Validation failure will suspend any subsequent service requests attempted by the TPP until the TPP's certificate or license issue is resolved.

# Release 2.7.0
> These release notes include all updates/changes introduced since version 2.5.0.

## *New Features and Enhancements*
This release introduces the following new features and enhancements:

- **Support for Transaction Filtering by Date-Range**
  > The `getTransactions()` method now filters transactions by date using `startDate` and `endDate` parameters to define the date range for the transactions sought. These date range parameters are optional. When specified, `startDate` is the inclusive lower bound of the transaction booking date and `endDate` is the inclusive upper bound. Each succeeding page of transactions will use the date range specified in the original request. 
  >
  > **NOTE**: Currently, this feature is supported in Java only, but will be available in other languages soon.

- **Support for Future-dated Payments and Standing Orders (recurring payments)**
  > Via the SDK, Token's connect services now provides the mechanism for a PSU to initiate instructions for a series of payments of a specified amount to a designated payee on a future date or at regularly scheduled intervals (daily, weekly, twice-weekly, monthly, twice-monthly, quarterly, semiannually, or yearly). Standing orders and future-dated payments implement the same workflow with the following highlights:
  > - **Standing Order**
  >   - Creates a new standing order payload in `tokenRequest` that includes `frequency`, `startDate` and `endDate`
  >   - PSU confirms the details, then clicks "Approve Standing Order" based on the standing order payload content displayed
  > - **Future-dated Payment**
  >   - Creates the transaction payload as a standard payment, but adds a specific `executionDate` in the future 
  >   - PSU confirms future-dated payment initiation based on the payload content displayed
    
- **Support for Confirming Available Funds (CAF)**
  > CAF checks that an account (debit/credit) has sufficient funds available to make a payment. The method for invoking Token's CAF feature differs somewhat between CBPIIs and PISPs.
  > - **Card-based Payment Instrument Issuer (CBPII)** — call the `confirmFunds()` function with the corresponding access token approved by the PSU, specifying the relevant account, amount and currency to receive a boolean indication that sufficient funds are in fact available.
  > - **Payment Information Service Provider (PISP)** — create a standard `TokenRequest` using the `setConfirmFunds()`  before executing the transfer request. Banks that do not support CAF will ignore `setConfirmFunds()`.
  
- **Augmented permissions (consent) list with support for new endpoints**
  > Additional PSU consent granularity is now implemented via the SDK:
  > - **Account Info**: `ReadAccountsBasic`, `ReadAccountsDetail`
  > - **Balances**: `ReadBalances`
  > - **Beneficiaries**: `ReadBeneficiariesBasic`, `ReadBeneficiariesDetail`
  > - **Direct Debits**: `ReadDirectDebits`
  > - **Offers**: `ReadOffers`
  > - **Party**: `ReadParty`
  > - **Products**: `ReadProducts`
  > - **Scheduled Payments**: `ReadScheduledPaymentsBasic`, `ReadScheduledPaymentsDetail`
  > - **Standing Orders**: `ReadStandingOrdersBasic`, `ReadStandingOrdersDetail`
  > - **Statements**: `ReadStatements Basic`, `ReadStatementsDetail`
  > - **Transactions**: `ReadTransactionsBasic`, `ReadTransactionsCredits`, `ReadTransactionsDebits`, `ReadTransactionsDetail`

- **TRUSTED_PARTY signature type removed from the directory service**
  > Replaced by a new `User_And_Realm` signature type for electronic signature by a trusted party
  
## *Deprecations*
Within a major version of the Token SDK, deprecated APIs/methods fall into two types:
  1. Functions removed/replaced by another function 
  2. Function that, though not removed, cannot be used in precisely the same way as previously (i.e., in a prior version of the SDK).
> **NOTE**: Version numbers consist of three numbers separated by dots. The leftmost number designates a **major** release. The middle number designates a **minor** (yet significant) update to a major release. The rightmost number identifies the package number of the SDK and is often referred to as a **point release**.

The deprecations listed here encompass the respective changes dating back to the original major version of the SDK and is intended for developers using the most current major version of the SDK, which at present is 2.7.0. Unless stated otherwise, deprecations apply to both asynchronous and synchronous variations of an API call. Developers using a version of the SDK predating the most current major release are encouraged to upgrade to take advantage of the latest capabilities and improvements.

- **`Member`**
  - `createAttachment` is deprecated; Token attachments are no longer supported
  - `redeemToken(... TransferEndpoint destination ...)`; use `redeemToken(... TransferDesitnation destination ...)` instead
  - `getTokenAttachment` is deprecated; Token attachments are no longer supported
  - `createCustomization(Payload logo, Map<String, Sting> colors, String consentText)` now accepts `String name` and `String appName` as well
  
- **`TokenClient`**
  - `getBanks(... Map,<String, Boolean> bank FeaturesMap ...)` is deprecated;
    use `getBanks(... BankFeatures bankFeatures ...)` instead
  - `completeRecoveryWithDefaultRule(String memberId, String verificationId, String code)` now accepts `CryptoEngine cryptoEngine` as well
  - `generateTokenRequestUrl(String requestId, String state)` should now set state on `TokenRequest` builder instead of passing it here
  - `generateTokenRequestUrl(String requestId, String state, String csrfToken)` should now set state and SDRF token on `TokenRequest` builder instead of passing them here
  - `parseTokenRequestCallbackIrl(String callbackUrl)` should now use the method that also takes in `String csrfToken'
  - `parseTokenRequestCallbackParams(Map<String, String. callbackParams)` should now use the method that also takes in `String csrfToken`

- **`TokenRequest (`/`TokenRequestBuilder`/[`Access`/`Transfer`]`TokenRequestBuilder)`**
  - `setProviderMetadata` is renamed to `setProviderTransferMetadata`
  - `addDestination(TransferEndpoint destination)` is deprecated; use `addDestination(TransferDestination destination)` instead

# Release 2.5.0

Introduces bulk transfers. This enables a list of transfers to be approved in a single consent.
To initiate a bulk transfer request, the TPP will create a token request from the 
`bulkTransferTokenRequestBuilder` method. The bulk transfer will be submitted to the bank
when the token is redeemed. Calling `getBulkTransfer` on the bulk transfer ID will return
 transaction IDs and statuses for the corresponding transactions from the bank.
 > **NOTE**: This feature is not yet fully supported by all banks pending agreed limits on file size and number of transfer items allowed.

# Release 2.3.6

Added method to update Transfer Destinations, only once, for a given token request in TPP sdk.
Added builder method `setSetTransferDestinationUrl` in TransferTokenBuilder
Added `parseSetTransferDestinationsUrl` method to parse `country` and `bank` selected by the users
from the callback url set in `setSetTransferDestinationUrl` builder method.

# Release 2.3.3

Provision to set `ChargeBearer` for a `TransferToken` is made available in `TokenRequest`.

# Release 1.1.23

The `login` methods in TokenIO and TokenIOAsync were removed.

# Release 1.0.84

The `login` methods in TokenIO and TokenIOAsync were renamed to `getMember`.

# Release 1.0.79

MemberAsync.aliases() and MemberAsync.firstAlias() now return
Observables instead of values. Member and MemberAsync used to
cache their aliases; now they fetch them from the cloud as needed.

# Release 1.0.64

getBalance methods for Member and Account have been
replaced by separate methods for getAvailableBalance
and getCurrentBalance.

# Release 1.0.60

The TokenIO.notifyPaymentRequest method parameters changed.
The method now takes one parameter, the TokenPayload.
