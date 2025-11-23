package model;

import utils.Logger;

public class BlockValidator {

    private static final Logger logger = Logger.getInstance();

    public static boolean validateBlock(Block block, int difficulty) {
        if (!block.getHash().equals(block.calculateHash())) {
            logger.error("Hash inválido en bloque #" + block.getIndex());
            return false;
        }

        if (!block.hasValidTransactions()) {
            logger.error("Transacciones inválidas en bloque #" + block.getIndex());
            return false;
        }

        String target = new String(new char[difficulty]).replace('\0', '0');
        if (!block.getHash().substring(0, Math.min(difficulty, block.getHash().length())).equals(target)) {
            logger.error("Dificultad no cumplida en bloque #" + block.getIndex());
            return false;
        }

        logger.debug("Bloque #" + block.getIndex() + " validado correctamente");
        return true;
    }
    
    public static boolean validateBlockStructure(Block block) {
         if (!block.getHash().equals(block.calculateHash())) {
            return false;
        }
        return block.hasValidTransactions();
    }
}
