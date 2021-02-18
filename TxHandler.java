import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {

     private UTXOPool utxoPool;
   /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        Set<UTXO> claimedUTXO = new HashSet<UTXO>();
		double inputTotal = 0;
		double outputTotal = 0;
        List<Transaction.Input> inputs = tx.getInputs();
		for (int i = 0; i < inputs.size(); i++) {
			Transaction.Input input = inputs.get(i);
            /**(1) all outputs claimed by {@code tx} are in the current UTXO pool. If not, return false **/
            if(!(utxoPool.contains(new UTXO(input.prevTxHash, input.outputIndex))))
				return false;
           /** (2) the signatures on each input of {@code tx} are valid,**/
			if (!verifySignatureOfCoinBeingUsed(tx, i, input)) {
				return false;
			}
            /**(3) no UTXO is claimed multiple times by {@code tx}. If the input is not in utxo pool, it is used. return false **/
			if (isUsedMultipleTimes(claimedUTXO, input)) {
				return false;
			}
            /*Compute input total  to complete the last 2 parts of exercise for this method*/
      		UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
			Transaction.Output correspondingOutput = utxoPool.getTxOutput(utxo);
			inputTotal += correspondingOutput.value;
		}
        /* Compute output total */
		List<Transaction.Output> outputs = tx.getOutputs();
		for (int i = 0; i < outputs.size(); i++) {
			Transaction.Output output = outputs.get(i);
            /**(4) all of {@code tx}s output values are non-negative*/
			if (output.value <= 0) {
				return false;
			}
			outputTotal += output.value;
		}
		// If outputTotal is more than input, it is not legit.
          /* * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output. **/
		if (outputTotal > inputTotal) {
			return false;
    }
    return true;
  }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> legitTransaction = new ArrayList<Transaction>();
        for (int i = 0; i < possibleTxs.length; i++) {
          Transaction tx = possibleTxs[i];
          if (isValidTx(tx)) {
              legitTransaction.add(tx);
          }
          //Txn is legit, now do book keeping and update UTXO pool
          List<Transaction.Input> inputs = tx.getInputs();
  		    for (int j = 0; j < inputs.size(); j++) {
                Transaction.Input input = inputs.get(j);
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                utxoPool.removeUTXO(utxo);//remove used coins from available pool
              }
          List<Transaction.Output> outputs = tx.getOutputs();
  		    for (int j = 0; j < outputs.size(); j++) {
                  Transaction.Output output = outputs.get(j);
                  UTXO utxo = new UTXO(tx.getHash(), j);
                  utxoPool.addUTXO(utxo, output);//add created coins to available pool
                }
          }
          Transaction[] resp = new Transaction[legitTransaction.size()];
          legitTransaction.toArray(resp);
          return resp; // Return array of valid tx
    }

    /****----- Private methods -----****/
    private boolean verifySignatureOfCoinBeingUsed(Transaction tx, int index, Transaction.Input input) {
		UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
		Transaction.Output correspondingOutput = utxoPool.getTxOutput(utxo);
		return Crypto.verifySignature(correspondingOutput.address, tx.getRawDataToSign(index), input.signature);
	}
    private boolean isUsedMultipleTimes(Set<UTXO> claimedUTXO, Transaction.Input input) {
		UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
		return !claimedUTXO.add(utxo);
	}

}
