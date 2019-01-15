/**
 * Copyright (c) 2017 Token, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.token.user.rpc;

import static com.google.common.base.Strings.nullToEmpty;
import static io.token.proto.ProtoJson.toJson;
import static io.token.proto.banklink.Banklink.AccountLinkingStatus.FAILURE_BANK_AUTHORIZATION_REQUIRED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.CANCELLED;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.ENDORSED;
import static io.token.proto.common.transaction.TransactionProtos.RequestStatus.SUCCESSFUL_REQUEST;
import static io.token.rpc.util.Converters.toCompletable;
import static io.token.user.util.Util.toObservable;
import static java.util.Arrays.asList;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.rpc.ClientFactory;
import io.token.rpc.UnauthenticatedClient;
import io.token.security.CryptoEngine;
import io.token.user.DeviceInfo;
import io.token.user.TransferTokenException;
import io.token.user.exceptions.BankAuthorizationRequiredException;
import io.token.user.exceptions.StepUpRequiredException;
import io.token.proto.PagedList;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.banklink.Banklink.OauthBankAuthorization;
import io.token.proto.common.account.AccountProtos.Account;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos.BankInfo;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.blob.BlobProtos.Blob.Payload;
import io.token.proto.common.member.MemberProtos.AddressRecord;
import io.token.proto.common.member.MemberProtos.Device;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation.Authorization;
import io.token.proto.common.member.MemberProtos.MemberRecoveryRulesOperation;
import io.token.proto.common.member.MemberProtos.MemberUpdate;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.member.MemberProtos.ProfilePictureSize;
import io.token.proto.common.member.MemberProtos.ReceiptContact;
import io.token.proto.common.member.MemberProtos.RecoveryRule;
import io.token.proto.common.member.MemberProtos.TrustedBeneficiary;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.notification.NotificationProtos;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.notification.NotificationProtos.StepUp;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.SecurityMetadata;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenRequestOptions;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload;
import io.token.proto.common.token.TokenProtos.TokenRequestStatePayload;
import io.token.proto.common.token.TokenProtos.TokenSignature.Action;
import io.token.proto.common.token.TokenProtos.TransferTokenStatus;
import io.token.proto.common.transaction.TransactionProtos.Balance;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transfer.TransferProtos.TransferPayload;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.proto.gateway.Gateway.AddAddressRequest;
import io.token.proto.gateway.Gateway.AddAddressResponse;
import io.token.proto.gateway.Gateway.AddTrustedBeneficiaryRequest;
import io.token.proto.gateway.Gateway.ApplyScaRequest;
import io.token.proto.gateway.Gateway.CancelTokenRequest;
import io.token.proto.gateway.Gateway.CancelTokenResponse;
import io.token.proto.gateway.Gateway.CreateAccessTokenRequest;
import io.token.proto.gateway.Gateway.CreateAccessTokenResponse;
import io.token.proto.gateway.Gateway.CreateBlobRequest;
import io.token.proto.gateway.Gateway.CreateBlobResponse;
import io.token.proto.gateway.Gateway.CreateCustomizationRequest;
import io.token.proto.gateway.Gateway.CreateCustomizationResponse;
import io.token.proto.gateway.Gateway.CreateTestBankAccountRequest;
import io.token.proto.gateway.Gateway.CreateTestBankAccountResponse;
import io.token.proto.gateway.Gateway.CreateTransferRequest;
import io.token.proto.gateway.Gateway.CreateTransferResponse;
import io.token.proto.gateway.Gateway.CreateTransferTokenRequest;
import io.token.proto.gateway.Gateway.CreateTransferTokenResponse;
import io.token.proto.gateway.Gateway.DeleteAddressRequest;
import io.token.proto.gateway.Gateway.DeleteMemberRequest;
import io.token.proto.gateway.Gateway.EndorseTokenRequest;
import io.token.proto.gateway.Gateway.EndorseTokenResponse;
import io.token.proto.gateway.Gateway.GetAccountRequest;
import io.token.proto.gateway.Gateway.GetAccountResponse;
import io.token.proto.gateway.Gateway.GetAccountsRequest;
import io.token.proto.gateway.Gateway.GetAccountsResponse;
import io.token.proto.gateway.Gateway.GetActiveAccessTokenRequest;
import io.token.proto.gateway.Gateway.GetActiveAccessTokenResponse;
import io.token.proto.gateway.Gateway.GetAddressRequest;
import io.token.proto.gateway.Gateway.GetAddressResponse;
import io.token.proto.gateway.Gateway.GetAddressesRequest;
import io.token.proto.gateway.Gateway.GetAddressesResponse;
import io.token.proto.gateway.Gateway.GetAliasesRequest;
import io.token.proto.gateway.Gateway.GetAliasesResponse;
import io.token.proto.gateway.Gateway.GetBalanceRequest;
import io.token.proto.gateway.Gateway.GetBalanceResponse;
import io.token.proto.gateway.Gateway.GetBalancesRequest;
import io.token.proto.gateway.Gateway.GetBalancesResponse;
import io.token.proto.gateway.Gateway.GetBankInfoRequest;
import io.token.proto.gateway.Gateway.GetBankInfoResponse;
import io.token.proto.gateway.Gateway.GetBlobRequest;
import io.token.proto.gateway.Gateway.GetBlobResponse;
import io.token.proto.gateway.Gateway.GetDefaultAccountRequest;
import io.token.proto.gateway.Gateway.GetDefaultAccountResponse;
import io.token.proto.gateway.Gateway.GetDefaultAgentRequest;
import io.token.proto.gateway.Gateway.GetDefaultAgentResponse;
import io.token.proto.gateway.Gateway.GetMemberRequest;
import io.token.proto.gateway.Gateway.GetMemberResponse;
import io.token.proto.gateway.Gateway.GetNotificationRequest;
import io.token.proto.gateway.Gateway.GetNotificationResponse;
import io.token.proto.gateway.Gateway.GetNotificationsRequest;
import io.token.proto.gateway.Gateway.GetNotificationsResponse;
import io.token.proto.gateway.Gateway.GetPairedDevicesRequest;
import io.token.proto.gateway.Gateway.GetPairedDevicesResponse;
import io.token.proto.gateway.Gateway.GetProfilePictureRequest;
import io.token.proto.gateway.Gateway.GetProfilePictureResponse;
import io.token.proto.gateway.Gateway.GetProfileRequest;
import io.token.proto.gateway.Gateway.GetProfileResponse;
import io.token.proto.gateway.Gateway.GetReceiptContactRequest;
import io.token.proto.gateway.Gateway.GetReceiptContactResponse;
import io.token.proto.gateway.Gateway.GetSubscriberRequest;
import io.token.proto.gateway.Gateway.GetSubscriberResponse;
import io.token.proto.gateway.Gateway.GetSubscribersRequest;
import io.token.proto.gateway.Gateway.GetSubscribersResponse;
import io.token.proto.gateway.Gateway.GetTokenBlobRequest;
import io.token.proto.gateway.Gateway.GetTokenBlobResponse;
import io.token.proto.gateway.Gateway.GetTokenRequest;
import io.token.proto.gateway.Gateway.GetTokenResponse;
import io.token.proto.gateway.Gateway.GetTokensRequest;
import io.token.proto.gateway.Gateway.GetTokensResponse;
import io.token.proto.gateway.Gateway.GetTransactionRequest;
import io.token.proto.gateway.Gateway.GetTransactionResponse;
import io.token.proto.gateway.Gateway.GetTransactionsRequest;
import io.token.proto.gateway.Gateway.GetTransactionsResponse;
import io.token.proto.gateway.Gateway.GetTransferRequest;
import io.token.proto.gateway.Gateway.GetTransferResponse;
import io.token.proto.gateway.Gateway.GetTransfersRequest;
import io.token.proto.gateway.Gateway.GetTransfersResponse;
import io.token.proto.gateway.Gateway.GetTrustedBeneficiariesRequest;
import io.token.proto.gateway.Gateway.GetTrustedBeneficiariesResponse;
import io.token.proto.gateway.Gateway.LinkAccountsOauthRequest;
import io.token.proto.gateway.Gateway.LinkAccountsOauthResponse;
import io.token.proto.gateway.Gateway.LinkAccountsRequest;
import io.token.proto.gateway.Gateway.LinkAccountsResponse;
import io.token.proto.gateway.Gateway.Page;
import io.token.proto.gateway.Gateway.RemoveTrustedBeneficiaryRequest;
import io.token.proto.gateway.Gateway.ReplaceTokenRequest;
import io.token.proto.gateway.Gateway.ReplaceTokenRequest.CancelToken;
import io.token.proto.gateway.Gateway.ReplaceTokenRequest.CreateToken;
import io.token.proto.gateway.Gateway.ReplaceTokenResponse;
import io.token.proto.gateway.Gateway.ResolveTransferDestinationsRequest;
import io.token.proto.gateway.Gateway.ResolveTransferDestinationsResponse;
import io.token.proto.gateway.Gateway.RetryVerificationRequest;
import io.token.proto.gateway.Gateway.RetryVerificationResponse;
import io.token.proto.gateway.Gateway.SetDefaultAccountRequest;
import io.token.proto.gateway.Gateway.SetProfilePictureRequest;
import io.token.proto.gateway.Gateway.SetProfileRequest;
import io.token.proto.gateway.Gateway.SetProfileResponse;
import io.token.proto.gateway.Gateway.SetReceiptContactRequest;
import io.token.proto.gateway.Gateway.SignTokenRequestStateRequest;
import io.token.proto.gateway.Gateway.SignTokenRequestStateResponse;
import io.token.proto.gateway.Gateway.StoreTokenRequestRequest;
import io.token.proto.gateway.Gateway.StoreTokenRequestResponse;
import io.token.proto.gateway.Gateway.SubscribeToNotificationsRequest;
import io.token.proto.gateway.Gateway.SubscribeToNotificationsResponse;
import io.token.proto.gateway.Gateway.TriggerStepUpNotificationRequest;
import io.token.proto.gateway.Gateway.TriggerStepUpNotificationResponse;
import io.token.proto.gateway.Gateway.UnlinkAccountsRequest;
import io.token.proto.gateway.Gateway.UnsubscribeFromNotificationsRequest;
import io.token.proto.gateway.Gateway.UpdateMemberRequest;
import io.token.proto.gateway.Gateway.UpdateMemberResponse;
import io.token.proto.gateway.Gateway.UpdateTokenRequestRequest;
import io.token.proto.gateway.Gateway.VerifyAffiliateRequest;
import io.token.proto.gateway.Gateway.VerifyAliasRequest;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.security.Signer;
import io.token.user.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;


/**
 * An authenticated RPC client that is used to talk to Token gateway. The
 * class is a thin wrapper on top of gRPC generated client. Makes the API
 * easier to use.
 */
public final class Client extends io.token.rpc.Client {
    /**
     * Creates a client instance.
     *
     * @param memberId member id
     * @param crypto the crypto engine used to sign for authentication, request payloads, etc
     * @param gateway gateway gRPC stub
     */
    protected Client(
            String memberId,
            CryptoEngine crypto,
            io.token.rpc.Client.GatewayProvider gateway) {
        super(memberId, crypto, gateway);
    }
}
