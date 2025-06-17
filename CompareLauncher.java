import java.io.File;

public class CompareLauncher {
    
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            afficherUsage();
            return;
        }
        
        String type = args[0].toLowerCase();
        String fichier1 = args[1];
        String fichier2 = args[2];
        
        // Vérifier l'existence des fichiers
        if (!verifierFichiers(fichier1, fichier2)) {
            return;
        }
        
        // Déléguer selon le type
        switch (type) {
            case "json":
                lancerComparaisonJson(args);
                break;
            case "req":
                lancerComparaisonReq(args);
                break;
            case "txt":
                lancerComparaisonTxt(args);
                break;
            default:
                System.out.println("Type non reconnu: " + type);
        }
    }
    
    private static void afficherUsage() {
        System.out.println("Usage:");
        System.out.println("  Pour JSON: json fichier1 fichier2 dossier [config.json]");
        System.out.println("  Pour REQ: req fichier1 fichier2 dossier");
        System.out.println("  Pour TXT: txt fichier1 fichier2 indexCol dossier");
    }
    
    private static boolean verifierFichiers(String fichier1, String fichier2) {
        if (!new File(fichier1).exists() || !new File(fichier2).exists()) {
            System.out.println("Erreur: Un des fichiers n'existe pas.");
            return false;
        }
        return true;
    }
    
    private static void lancerComparaisonJson(String[] args) throws Exception {
        String dossier = args.length >= 4 ? args[3] : ".";
        String config = args.length == 5 ? args[4] : "config.json";
        
        com.mmd.CompareJsonFiles.main(new String[]{args[1], args[2], dossier, config});
    }
    
    private static void lancerComparaisonReq(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Erreur: dossier de sortie manquant pour REQ.");
            return;
        }
        CompareReqFiles.main(new String[]{args[1], args[2], args[3]});
    }
    
    private static void lancerComparaisonTxt(String[] args) throws Exception {
        if (args.length < 5) {
            System.out.println("Erreur: index de colonne ou dossier manquant pour TXT.");
            return;
        }
        CompareTxtFiles.main(new String[]{args[1], args[2], args[3], args[4]});
    }
}
