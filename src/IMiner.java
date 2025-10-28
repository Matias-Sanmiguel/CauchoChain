public interface IMiner {
    public void mine(Blockchain bc);
    public boolean validateBlock (Block block);
}
