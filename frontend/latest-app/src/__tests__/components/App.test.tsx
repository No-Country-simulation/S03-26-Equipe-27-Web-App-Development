import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { server } from "../mocks/server";
import App from "../../App";

function renderApp() {
  return render(<App />);
}

// ─── Carregamento inicial ─────────────────────────────────────────────────────

describe("App — carregamento inicial", () => {

  it("deve exibir o título principal", async () => {
    renderApp();
    await waitFor(() =>
      expect(screen.getByText(/SmartTrafficFlow/i)).toBeInTheDocument()
    );
  });

  it("deve exibir status 'Conectado' após carregar", async () => {
    renderApp();
    await waitFor(() =>
      expect(screen.getByText(/Conectado/i)).toBeInTheDocument()
    );
  });

  it("deve exibir os 4 cards de resumo", async () => {
    renderApp();
    await waitFor(() => {
      expect(screen.getByText(/Veículos capturados/i)).toBeInTheDocument();
      expect(screen.getByText(/Regiões mapeadas/i)).toBeInTheDocument();
      expect(screen.getByText(/Carga média/i)).toBeInTheDocument();
      expect(screen.getByText(/Janela de pico/i)).toBeInTheDocument();
    });
  });

  it("deve exibir insights gerados pelo backend", async () => {
    renderApp();
    await waitFor(() =>
      expect(screen.getByText(/maior volume agregado/i)).toBeInTheDocument()
    );
  });

  it("deve renderizar o container do mapa", async () => {
    renderApp();
    await waitFor(() =>
      expect(screen.getByTestId("map-container")).toBeInTheDocument()
    );
  });

  it("deve exibir os botões de exportação", async () => {
    renderApp();
    await waitFor(() => {
      expect(screen.getByRole("button", { name: /Exportar CSV/i })).toBeInTheDocument();
      expect(screen.getByRole("button", { name: /Exportar JSON/i })).toBeInTheDocument();
    });
  });
});

// ─── Erros de backend ─────────────────────────────────────────────────────────

describe("App — erros de backend", () => {

  it("deve exibir mensagem de erro quando backend falha", async () => {
    server.use(
      http.get("http://localhost:8080/api/traffic-records", () =>
        HttpResponse.json({}, { status: 500 })
      )
    );
    renderApp();
    await waitFor(() =>
      expect(screen.getByText(/backend|atenção|erro/i)).toBeInTheDocument()
    );
  });

  it("não deve travar com banco vazio", async () => {
    server.use(
      http.get("http://localhost:8080/api/traffic-records", () => HttpResponse.json([])),
      http.get("http://localhost:8080/api/traffic-insights", () =>
        HttpResponse.json({ insights: [] })
      )
    );
    renderApp();
    await waitFor(() =>
      expect(screen.getByText(/Conectado/i)).toBeInTheDocument()
    );
  });

  it("deve exibir placeholder quando não há insights", async () => {
    server.use(
      http.get("http://localhost:8080/api/traffic-insights", () =>
        HttpResponse.json({ insights: [] })
      )
    );
    renderApp();
    await waitFor(() =>
      expect(screen.getByText(/Aguardando registros/i)).toBeInTheDocument()
    );
  });
});

// ─── Navegação entre views ────────────────────────────────────────────────────

describe("App — navegação entre views", () => {

  it("deve iniciar na view de insights com aria-pressed=true", async () => {
    renderApp();
    await waitFor(() => {
      const btn = screen.getByRole("button", { name: /Página de insights/i });
      expect(btn).toHaveAttribute("aria-pressed", "true");
    });
  });

  it("deve ir para view de formulários ao clicar", async () => {
    const user = userEvent.setup();
    renderApp();
    await waitFor(() => screen.getByText(/Formulários e registros/i));

    await user.click(screen.getByRole("button", { name: /Formulários e registros/i }));

    await waitFor(() =>
      expect(screen.getByText(/Adicionar registro de tráfego/i)).toBeInTheDocument()
    );
  });

  it("deve voltar para home ao clicar em Página de insights", async () => {
    const user = userEvent.setup();
    renderApp();
    await waitFor(() => screen.getByText(/Formulários e registros/i));

    await user.click(screen.getByRole("button", { name: /Formulários e registros/i }));
    await waitFor(() => screen.getByText(/Adicionar registro/i));

    await user.click(screen.getByRole("button", { name: /Página de insights/i }));
    await waitFor(() =>
      expect(screen.getByText(/Veículos capturados/i)).toBeInTheDocument()
    );
  });
});

// ─── Formulário de registro ───────────────────────────────────────────────────

describe("App — formulário de criação de registro", () => {

  async function goToWorkspace(user: ReturnType<typeof userEvent.setup>) {
    await waitFor(() => screen.getByText(/Formulários e registros/i));
    await user.click(screen.getByRole("button", { name: /Formulários e registros/i }));
    await waitFor(() => screen.getByText(/Adicionar registro de tráfego/i));
  }

  it("deve exibir todos os campos do formulário", async () => {
    const user = userEvent.setup();
    renderApp();
    await goToWorkspace(user);

    expect(screen.getByLabelText(/Data e hora/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Tipo de via/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Volume de veículos/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Região/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Tipo de evento/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Clima/i)).toBeInTheDocument();
  });

  it("deve desabilitar o botão durante o envio", async () => {
    const user = userEvent.setup();
    renderApp();
    await goToWorkspace(user);

    const btn = screen.getByRole("button", { name: /Salvar registro/i });
    await user.click(btn);
    expect(btn).toBeDisabled();
  });

  it("deve exibir mensagem de sucesso após salvar", async () => {
    const user = userEvent.setup();
    renderApp();
    await goToWorkspace(user);

    await user.click(screen.getByRole("button", { name: /Salvar registro/i }));
    await waitFor(() =>
      expect(screen.getByText(/registro de tráfego adicionado/i)).toBeInTheDocument()
    );
  });

  it("deve exibir erro quando backend retorna 400", async () => {
    server.use(
      http.post("http://localhost:8080/api/traffic-records", () =>
        HttpResponse.json({ code: "VALIDATION_ERROR", message: "Dados inválidos" }, { status: 400 })
      )
    );
    const user = userEvent.setup();
    renderApp();
    await goToWorkspace(user);

    await user.click(screen.getByRole("button", { name: /Salvar registro/i }));
    await waitFor(() =>
      expect(screen.getByText(/inválidos|Dados/i)).toBeInTheDocument()
    );
  });
});

// ─── Formulário de simulação ──────────────────────────────────────────────────

describe("App — formulário de simulação", () => {

  async function goToWorkspace(user: ReturnType<typeof userEvent.setup>) {
    await waitFor(() => screen.getByText(/Formulários e registros/i));
    await user.click(screen.getByRole("button", { name: /Formulários e registros/i }));
    await waitFor(() => screen.getByText(/Gerar tráfego simulado/i));
  }

  it("deve exibir campos do formulário de simulação", async () => {
    const user = userEvent.setup();
    renderApp();
    await goToWorkspace(user);

    expect(screen.getByLabelText(/Nome do cenário/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Registros a gerar/i)).toBeInTheDocument();
  });

  it("deve executar e exibir contagem de registros gerados", async () => {
    const user = userEvent.setup();
    renderApp();
    await goToWorkspace(user);

    await user.click(screen.getByRole("button", { name: /Executar simulação/i }));
    await waitFor(() =>
      expect(screen.getByText(/gerou \d+ novos registros/i)).toBeInTheDocument()
    );
  });

  it("deve desabilitar o botão durante a geração", async () => {
    const user = userEvent.setup();
    renderApp();
    await goToWorkspace(user);

    const btn = screen.getByRole("button", { name: /Executar simulação/i });
    await user.click(btn);
    expect(btn).toBeDisabled();
  });
});

// ─── Tabela e filtro ──────────────────────────────────────────────────────────

describe("App — tabela de registros e filtro", () => {

  async function goToWorkspace(user: ReturnType<typeof userEvent.setup>) {
    await waitFor(() => screen.getByText(/Formulários e registros/i));
    await user.click(screen.getByRole("button", { name: /Formulários e registros/i }));
    await waitFor(() => screen.getByText(/Registros recebidos/i));
  }

  it("deve exibir registros do backend na tabela", async () => {
    const user = userEvent.setup();
    renderApp();
    await goToWorkspace(user);

    await waitFor(() => {
      expect(screen.getByText(/Arterial/i)).toBeInTheDocument();
      expect(screen.getByText(/Highway/i)).toBeInTheDocument();
    });
  });

  it("deve filtrar registros ao digitar", async () => {
    const user = userEvent.setup();
    renderApp();
    await goToWorkspace(user);
    await waitFor(() => screen.getByText(/Arterial/i));

    await user.type(screen.getByPlaceholderText(/Buscar por região/i), "NORTE");

    await waitFor(() => {
      expect(screen.queryByText(/^Arterial$/i)).not.toBeInTheDocument();
      expect(screen.getByText(/Highway/i)).toBeInTheDocument();
    });
  });

  it("deve exibir estado vazio quando filtro não corresponde", async () => {
    const user = userEvent.setup();
    renderApp();
    await goToWorkspace(user);
    await waitFor(() => screen.getByText(/Arterial/i));

    await user.type(
      screen.getByPlaceholderText(/Buscar por região/i),
      "XXXXXXXXXXXXXXXXXXX"
    );

    await waitFor(() =>
      expect(screen.getByText(/Nenhum registro corresponde/i)).toBeInTheDocument()
    );
  });
});

// ─── Exportação ───────────────────────────────────────────────────────────────

describe("App — exportação de dados", () => {

  it("deve exibir mensagem de sucesso após exportar CSV", async () => {
    URL.createObjectURL = vi.fn().mockReturnValue("blob:test");
    URL.revokeObjectURL = vi.fn();

    const user = userEvent.setup();
    renderApp();
    await waitFor(() => screen.getByRole("button", { name: /Exportar CSV/i }));

    await user.click(screen.getByRole("button", { name: /Exportar CSV/i }));
    await waitFor(() =>
      expect(screen.getByText(/Exportação preparada.*CSV/i)).toBeInTheDocument()
    );
  });
});

// ─── Acessibilidade ───────────────────────────────────────────────────────────

describe("App — acessibilidade", () => {

  it("deve ter skip-link 'Pular para o conteúdo'", async () => {
    renderApp();
    await waitFor(() =>
      expect(screen.getByText(/Pular para o conteúdo/i)).toBeInTheDocument()
    );
  });

  it("deve ter #main-content no DOM", async () => {
    renderApp();
    await waitFor(() =>
      expect(document.getElementById("main-content")).not.toBeNull()
    );
  });

  it("deve ter botões de navegação com aria-pressed", async () => {
    renderApp();
    await waitFor(() => {
      screen.getAllByRole("button", { name: /insights|formulários/i })
        .forEach(btn => expect(btn).toHaveAttribute("aria-pressed"));
    });
  });

  it("deve ter pelo menos uma região com aria-live='polite'", async () => {
    renderApp();
    await waitFor(() => {
      const liveRegions = document.querySelectorAll("[aria-live='polite']");
      expect(liveRegions.length).toBeGreaterThan(0);
    });
  });
});
