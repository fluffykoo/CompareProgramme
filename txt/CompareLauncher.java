package com.mmd;

import com.mmd.json.CompareJsonFiles;
import com.mmd.req.CompareReqFiles;
import com.mmd.txt.CompareTxtFiles;

import java.io.File;

public class CompareLauncher {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage :");
            System.out.println("  Pour JSON :  json <fichier1> <fichier2> <dossier> [config.json]");
            System.out.println("  Pour REQ  :  req <fichier1> <fichier2> <dossier>");
            System.out.println("  Pour TXT  :  txt <fichier1> <fichier2> <indexCol> <dossier> [terminal]");
            return;
        }

        String type = args[0].toLowerCase();
        String file1 = args[1];
        String file2 = args[2];

        if (!new File(file1).exists() || !new File(file2).exists()) {
            System.out.println("Erreur : Un des fichiers n’existe pas.");
            return;
        }

        switch (type) {
            case "json":
                if (args.length < 4) {
                    System.out.println("Erreur : dossier manquant.");
                    return;
                }
                String jsonFolder = args[3];
                String config = args.length >= 5 ? args[4] : "config.json";
                CompareJsonFiles.main(new String[]{file1, file2, jsonFolder, config});
                break;

            case "req":
                if (args.length < 4) {
                    System.out.println("Erreur : dossier manquant.");
                    return;
                }
                String reqFolder = args[3];
                CompareReqFiles.main(new String[]{file1, file2, reqFolder});
                break;

            case "txt":
                if (args.length < 5) {
                    System.out.println("Erreur : indexCol ou dossier manquant.");
                    return;
                }
                String indexCol = args[3];
                String txtFolder = args[4];
                String terminal = args.length >= 6 ? args[5] : "";
                CompareTxtFiles.main(new String[]{file1, file2, indexCol, txtFolder, terminal});
                break;

            default:
                System.out.println("Type non reconnu : " + type);
        }
    }
}