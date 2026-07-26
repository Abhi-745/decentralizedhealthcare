package com.example.VeristasId.Blockchain;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple7;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.9.8.
 */
@SuppressWarnings("rawtypes")
public class VeristasAudit extends Contract {
    public static final String BINARY = "6080604052348015600e575f5ffd5b50610d238061001c5f395ff3fe608060405234801561000f575f5ffd5b506004361061004a575f3560e01c806316fe1eda1461004e5780636353bde614610063578063783b8bd014610078578063ddaf09291461009e575b5f5ffd5b61006161005c366004610970565b6100b1565b005b5f546040519081526020015b60405180910390f35b61008b610086366004610a62565b610213565b60405161006f9796959493929190610aa7565b61008b6100ac366004610a62565b6105d1565b6040805160e08101825242815260208101888152918101879052606081018690526080810185905260a0810184905282151560c08201525f8054600181018255908052815160079091027f290decd9548b62a8d60345a988386fc84ba6bc95484008f6362f93160ef3e56381019182559251919283927f290decd9548b62a8d60345a988386fc84ba6bc95484008f6362f93160ef3e564909101906101569082610ba8565b506040820151600282019061016b9082610ba8565b50606082015160038201906101809082610ba8565b50608082015160048201906101959082610ba8565b5060a082015160058201906101aa9082610ba8565b5060c091909101516006909101805460ff191691151591909117905560405142907fc7bc0afeb6abc9677d039274a669ebf8927a5f9a4db4f9d0698b09221100fe3890610202908a908a908a908a908a908a90610c63565b60405180910390a250505050505050565b5f60608060608060605f5f8054905088106102865760405162461bcd60e51b815260206004820152602960248201527f566572697374617341756469743a207265636f726420696e646578206f7574206044820152686f6620626f756e647360b81b606482015260840160405180910390fd5b5f5f898154811061029957610299610cd9565b905f5260205f2090600702016040518060e00160405290815f82015481526020016001820180546102c990610b24565b80601f01602080910402602001604051908101604052809291908181526020018280546102f590610b24565b80156103405780601f1061031757610100808354040283529160200191610340565b820191905f5260205f20905b81548152906001019060200180831161032357829003601f168201915b5050505050815260200160028201805461035990610b24565b80601f016020809104026020016040519081016040528092919081815260200182805461038590610b24565b80156103d05780601f106103a7576101008083540402835291602001916103d0565b820191905f5260205f20905b8154815290600101906020018083116103b357829003601f168201915b505050505081526020016003820180546103e990610b24565b80601f016020809104026020016040519081016040528092919081815260200182805461041590610b24565b80156104605780601f1061043757610100808354040283529160200191610460565b820191905f5260205f20905b81548152906001019060200180831161044357829003601f168201915b5050505050815260200160048201805461047990610b24565b80601f01602080910402602001604051908101604052809291908181526020018280546104a590610b24565b80156104f05780601f106104c7576101008083540402835291602001916104f0565b820191905f5260205f20905b8154815290600101906020018083116104d357829003601f168201915b5050505050815260200160058201805461050990610b24565b80601f016020809104026020016040519081016040528092919081815260200182805461053590610b24565b80156105805780601f1061055757610100808354040283529160200191610580565b820191905f5260205f20905b81548152906001019060200180831161056357829003601f168201915b50505091835250506006919091015460ff16151560209182015281519082015160408301516060840151608085015160a086015160c090960151949f939e50919c509a509850919650945092505050565b5f81815481106105df575f80fd5b5f918252602090912060079091020180546001820180549193509061060390610b24565b80601f016020809104026020016040519081016040528092919081815260200182805461062f90610b24565b801561067a5780601f106106515761010080835404028352916020019161067a565b820191905f5260205f20905b81548152906001019060200180831161065d57829003601f168201915b50505050509080600201805461068f90610b24565b80601f01602080910402602001604051908101604052809291908181526020018280546106bb90610b24565b80156107065780601f106106dd57610100808354040283529160200191610706565b820191905f5260205f20905b8154815290600101906020018083116106e957829003601f168201915b50505050509080600301805461071b90610b24565b80601f016020809104026020016040519081016040528092919081815260200182805461074790610b24565b80156107925780601f1061076957610100808354040283529160200191610792565b820191905f5260205f20905b81548152906001019060200180831161077557829003601f168201915b5050505050908060040180546107a790610b24565b80601f01602080910402602001604051908101604052809291908181526020018280546107d390610b24565b801561081e5780601f106107f55761010080835404028352916020019161081e565b820191905f5260205f20905b81548152906001019060200180831161080157829003601f168201915b50505050509080600501805461083390610b24565b80601f016020809104026020016040519081016040528092919081815260200182805461085f90610b24565b80156108aa5780601f10610881576101008083540402835291602001916108aa565b820191905f5260205f20905b81548152906001019060200180831161088d57829003601f168201915b5050506006909301549192505060ff1687565b634e487b7160e01b5f52604160045260245ffd5b5f82601f8301126108e0575f5ffd5b813567ffffffffffffffff8111156108fa576108fa6108bd565b604051601f8201601f19908116603f0116810167ffffffffffffffff81118282101715610929576109296108bd565b604052818152838201602001851015610940575f5ffd5b816020850160208301375f918101602001919091529392505050565b8035801515811461096b575f5ffd5b919050565b5f5f5f5f5f5f60c08789031215610985575f5ffd5b863567ffffffffffffffff81111561099b575f5ffd5b6109a789828a016108d1565b965050602087013567ffffffffffffffff8111156109c3575f5ffd5b6109cf89828a016108d1565b955050604087013567ffffffffffffffff8111156109eb575f5ffd5b6109f789828a016108d1565b945050606087013567ffffffffffffffff811115610a13575f5ffd5b610a1f89828a016108d1565b935050608087013567ffffffffffffffff811115610a3b575f5ffd5b610a4789828a016108d1565b925050610a5660a0880161095c565b90509295509295509295565b5f60208284031215610a72575f5ffd5b5035919050565b5f81518084528060208401602086015e5f602082860101526020601f19601f83011685010191505092915050565b87815260e060208201525f610abf60e0830189610a79565b8281036040840152610ad18189610a79565b90508281036060840152610ae58188610a79565b90508281036080840152610af98187610a79565b905082810360a0840152610b0d8186610a79565b91505082151560c083015298975050505050505050565b600181811c90821680610b3857607f821691505b602082108103610b5657634e487b7160e01b5f52602260045260245ffd5b50919050565b601f821115610ba357805f5260205f20601f840160051c81016020851015610b815750805b601f840160051c820191505b81811015610ba0575f8155600101610b8d565b50505b505050565b815167ffffffffffffffff811115610bc257610bc26108bd565b610bd681610bd08454610b24565b84610b5c565b6020601f821160018114610c08575f8315610bf15750848201515b5f19600385901b1c1916600184901b178455610ba0565b5f84815260208120601f198516915b82811015610c375787850151825560209485019460019092019101610c17565b5084821015610c5457868401515f19600387901b60f8161c191681555b50505050600190811b01905550565b60c081525f610c7560c0830189610a79565b8281036020840152610c878189610a79565b90508281036040840152610c9b8188610a79565b90508281036060840152610caf8187610a79565b90508281036080840152610cc38186610a79565b91505082151560a0830152979650505050505050565b634e487b7160e01b5f52603260045260245ffdfea264697066735822122009631a2734c07c65bef0f6bb30739f66af6eb81840626ca4bcaa06f3aefc961f64736f6c634300081e0033";

    public static final String FUNC_AUDITTRAIL = "auditTrail";

    public static final String FUNC_GETAUDITCOUNT = "getAuditCount";

    public static final String FUNC_GETAUDITRECORD = "getAuditRecord";

    public static final String FUNC_LOGACCESS = "logAccess";

    public static final Event ACCESSLOGGED_EVENT = new Event("AccessLogged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Bool>() {}));
    ;

    @Deprecated
    protected VeristasAudit(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected VeristasAudit(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected VeristasAudit(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected VeristasAudit(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<AccessLoggedEventResponse> getAccessLoggedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ACCESSLOGGED_EVENT, transactionReceipt);
        ArrayList<AccessLoggedEventResponse> responses = new ArrayList<AccessLoggedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AccessLoggedEventResponse typedResponse = new AccessLoggedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.timestamp = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.institutionDid = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.userDid = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.roleTag = (String) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.action = (String) eventValues.getNonIndexedValues().get(3).getValue();
            typedResponse.stage = (String) eventValues.getNonIndexedValues().get(4).getValue();
            typedResponse.accessGranted = (Boolean) eventValues.getNonIndexedValues().get(5).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AccessLoggedEventResponse getAccessLoggedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ACCESSLOGGED_EVENT, log);
        AccessLoggedEventResponse typedResponse = new AccessLoggedEventResponse();
        typedResponse.log = log;
        typedResponse.timestamp = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.institutionDid = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.userDid = (String) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.roleTag = (String) eventValues.getNonIndexedValues().get(2).getValue();
        typedResponse.action = (String) eventValues.getNonIndexedValues().get(3).getValue();
        typedResponse.stage = (String) eventValues.getNonIndexedValues().get(4).getValue();
        typedResponse.accessGranted = (Boolean) eventValues.getNonIndexedValues().get(5).getValue();
        return typedResponse;
    }

    public Flowable<AccessLoggedEventResponse> accessLoggedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAccessLoggedEventFromLog(log));
    }

    public Flowable<AccessLoggedEventResponse> accessLoggedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ACCESSLOGGED_EVENT));
        return accessLoggedEventFlowable(filter);
    }

    public RemoteFunctionCall<Tuple7<BigInteger, String, String, String, String, String, Boolean>> auditTrail(BigInteger param0) {
        final Function function = new Function(FUNC_AUDITTRAIL, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Bool>() {}));
        return new RemoteFunctionCall<Tuple7<BigInteger, String, String, String, String, String, Boolean>>(function,
                new Callable<Tuple7<BigInteger, String, String, String, String, String, Boolean>>() {
                    @Override
                    public Tuple7<BigInteger, String, String, String, String, String, Boolean> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple7<BigInteger, String, String, String, String, String, Boolean>(
                                (BigInteger) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (String) results.get(2).getValue(), 
                                (String) results.get(3).getValue(), 
                                (String) results.get(4).getValue(), 
                                (String) results.get(5).getValue(), 
                                (Boolean) results.get(6).getValue());
                    }
                });
    }

    public RemoteFunctionCall<BigInteger> getAuditCount() {
        final Function function = new Function(FUNC_GETAUDITCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple7<BigInteger, String, String, String, String, String, Boolean>> getAuditRecord(BigInteger index) {
        final Function function = new Function(FUNC_GETAUDITRECORD, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(index)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Bool>() {}));
        return new RemoteFunctionCall<Tuple7<BigInteger, String, String, String, String, String, Boolean>>(function,
                new Callable<Tuple7<BigInteger, String, String, String, String, String, Boolean>>() {
                    @Override
                    public Tuple7<BigInteger, String, String, String, String, String, Boolean> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple7<BigInteger, String, String, String, String, String, Boolean>(
                                (BigInteger) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (String) results.get(2).getValue(), 
                                (String) results.get(3).getValue(), 
                                (String) results.get(4).getValue(), 
                                (String) results.get(5).getValue(), 
                                (Boolean) results.get(6).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> logAccess(String _institutionDid, String _userDid, String _roleTag, String _action, String _stage, Boolean _accessGranted) {
        final Function function = new Function(
                FUNC_LOGACCESS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_institutionDid), 
                new org.web3j.abi.datatypes.Utf8String(_userDid), 
                new org.web3j.abi.datatypes.Utf8String(_roleTag), 
                new org.web3j.abi.datatypes.Utf8String(_action), 
                new org.web3j.abi.datatypes.Utf8String(_stage), 
                new org.web3j.abi.datatypes.Bool(_accessGranted)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static VeristasAudit load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new VeristasAudit(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static VeristasAudit load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new VeristasAudit(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static VeristasAudit load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new VeristasAudit(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static VeristasAudit load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new VeristasAudit(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<VeristasAudit> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(VeristasAudit.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<VeristasAudit> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(VeristasAudit.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<VeristasAudit> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(VeristasAudit.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<VeristasAudit> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(VeristasAudit.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class AccessLoggedEventResponse extends BaseEventResponse {
        public BigInteger timestamp;

        public String institutionDid;

        public String userDid;

        public String roleTag;

        public String action;

        public String stage;

        public Boolean accessGranted;
    }
}
