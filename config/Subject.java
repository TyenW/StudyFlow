package config;

public class Subject {
    public String originalLine;
    public String statusChar;
    public String name;
    public String period;

    public Subject(String originalLine, String statusChar, String name, String period) {
        this.originalLine = originalLine;
        this.statusChar = statusChar;
        this.name = name;
        this.period = period;
    }

    public String getSanitizedFolderName() {
        String clean = name.replaceAll("\\*\\s*$", "").trim();
        // Substitui caracteres inválidos para nomes de arquivos/pastas no Windows (: * ? " < > | \ /)
        clean = clean.replaceAll("[:\\\\/*?\"<>|]", " ");
        // Limpa espaços duplicados e traços nas pontas se houver
        clean = clean.replaceAll("\\s+", " ").replaceAll("^-|-$", "").trim();
        return clean;
    }
}
