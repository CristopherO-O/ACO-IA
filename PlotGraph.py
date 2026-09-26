import os
import csv
import matplotlib.pyplot as plt

# Nome do arquivo -> (rótulo, nome da imagem de saída)
ARQUIVOS = {
    "convergencia_facil.csv": ("Fácil", "convergencia_facil.png"),
    "convergencia_med.csv": ("Médio", "convergencia_med.png"),
    "convergencia_dif.csv": ("Difícil", "convergencia_dif.png"),
}

PASTA = os.path.dirname(os.path.abspath(__file__))


def ler_csv(caminho):
    iteracoes = []
    makespans = []
    with open(caminho, newline="", encoding="utf-8") as f:
        leitor = csv.DictReader(f)
        for linha in leitor:
            iteracoes.append(int(linha["iteracao"]))
            makespans.append(float(linha["melhor_makespan"]))
    return iteracoes, makespans


def gerar_grafico(nome_arquivo, rotulo, nome_saida):
    caminho_csv = os.path.join(PASTA, nome_arquivo)
    if not os.path.isfile(caminho_csv):
        print(f"[AVISO] Arquivo não encontrado, pulando: {nome_arquivo}")
        return

    iteracoes, makespans = ler_csv(caminho_csv)
    melhor_global = min(makespans)

    plt.figure(figsize=(9, 6))
    plt.plot(iteracoes, makespans, label="Makespan por iteração", linewidth=2, color="tab:blue")
    plt.axhline(
        melhor_global,
        color="tab:red",
        linestyle="--",
        linewidth=1.5,
        label=f"Melhor global ({melhor_global:.2f})",
    )

    plt.title(f"Convergência do Makespan - Instância {rotulo}")
    plt.xlabel("Iteração")
    plt.ylabel("Melhor Makespan")
    plt.legend()
    plt.grid(True, alpha=0.3)
    plt.tight_layout()

    caminho_saida = os.path.join(PASTA, nome_saida)
    plt.savefig(caminho_saida, dpi=200)
    plt.close()
    print(f"[OK] {nome_arquivo} -> {nome_saida}")


def main():
    algum_encontrado = False
    for nome_arquivo, (rotulo, nome_saida) in ARQUIVOS.items():
        if os.path.isfile(os.path.join(PASTA, nome_arquivo)):
            algum_encontrado = True
        gerar_grafico(nome_arquivo, rotulo, nome_saida)

    if not algum_encontrado:
        print("[ERRO] Nenhum dos arquivos CSV esperados foi encontrado na pasta:")
        for nome_arquivo in ARQUIVOS:
            print(f"  - {nome_arquivo}")


if __name__ == "__main__":
    main()