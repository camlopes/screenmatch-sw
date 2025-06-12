package br.com.alura.screenmatch.main;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoAPI;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Main {
    private Scanner scanner = new Scanner(System.in);
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = System.getenv("API_KEY");
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private ConverteDados conversor = new ConverteDados();
    private List<Serie> series = new ArrayList<>();
    private SerieRepository repository;
    private Optional<Serie> serieBuscada;

    public Main(SerieRepository repository) {
        this.repository = repository;
    }

    public void exibeMenu() {
        var opcao = -1;
        while (opcao != 0) {
            var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Historico de series buscadas
                    4 - Verificar series ja buscadas
                    5 - Buscar series por ator
                    6 - Listar as Top 5 series
                    7 - Buscar series por genero
                    8 - Filtrar séries por temporada e avaliação
                    9 - Buscar episodios por titulo
                    10 - Listar os Top 5 episodios
                    
                    0 - Sair
                    """;

            System.out.println(menu);
            System.out.print("Escolha uma das opçoes acima: ");
            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriesPorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriesPorGenero();
                    break;
                case 8:
                    filtrarSeriesPorTemporadaEAvaliacao();
                    break;
                case 9:
                    buscarEpisodioPorTitulo();
                    break;
                case 10:
                    buscarTop5Episodios();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    private void listarSeriesBuscadas() {
        series = repository.findAll();

        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        repository.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.print("Digite o nome da série para busca: ");
        var nomeSerie = scanner.nextLine();
        var json = consumoAPI.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie() {
        listarSeriesBuscadas();
        System.out.print("Escolha uma serie pelo nome: ");
        var nomeSerie = scanner.nextLine();
        List<DadosTemporada> temporadas = new ArrayList<>();

        Optional<Serie> serie = repository.findByTituloContainingIgnoreCase(nomeSerie);

        if (serie.isPresent()) {
            var serieEncontrada = serie.get();
            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumoAPI.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repository.save(serieEncontrada);
        } else {
            System.out.println("Serie não encontrada.");
        }
    }

    private void buscarSeriePorTitulo() {
        System.out.print("Escolha uma serie pelo nome: ");
        var nomeSerie = scanner.nextLine();
        serieBuscada = repository.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBuscada.isPresent()) {
            System.out.println("Dados da serie: " + serieBuscada.get());

        } else {
            System.out.println("Serie não encontrada.");
        }
    }

    private void buscarSeriesPorAtor() {
        System.out.print("Digite um nome de ator: ");
        var nomeAtor = scanner.nextLine();
        System.out.print("Digite o valor da avaliacao: ");
        var avaliacao = scanner.nextDouble();

        List<Serie> seriesEncontrada = repository.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);

        System.out.println("Series em que " + nomeAtor + " trabalhou:");
        seriesEncontrada.forEach(s ->
                System.out.println(s.getTitulo() + " Avaliacao: " + s.getAvaliacao()));
    }

    private void buscarTop5Series() {
        List<Serie> seriesTop = repository.findTop5ByOrderByAvaliacaoDesc();

        seriesTop.forEach(s ->
                System.out.println(s.getTitulo() + " Avaliacao: " + s.getAvaliacao()));
    }

    private void buscarSeriesPorGenero() {
        System.out.print("Digite um genero para a busca: ");
        var buscaGenero = scanner.nextLine();
        Categoria categoria = Categoria.fromPortugues(buscaGenero);
        List<Serie> seriesPorGenero = repository.findByGenero(categoria);
        System.out.println("Series do genero " + buscaGenero + ":");
        seriesPorGenero.forEach(System.out::println);
    }

    private void filtrarSeriesPorTemporadaEAvaliacao() {
        System.out.print("Digite o numero maximo de temporadas que a serie deve possuir: ");
        var totalTemporadas = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Digite o valor da avaliacao que a serie deve possuir: ");
        var avaliacao = scanner.nextDouble();
        scanner.nextLine();
        List<Serie> filtroSeries = repository.seriesPorTemporadaEAValiacao(totalTemporadas, avaliacao);
        System.out.println("Series encontradas com " + totalTemporadas + " temporadas e avaliacao: " + avaliacao);
        filtroSeries.forEach(s ->
                System.out.println(s.getTitulo() + "  - avaliação: " + s.getAvaliacao()));
    }

    private void buscarEpisodioPorTitulo() {
        System.out.print("Digite um nome de episodio: ");
        var nomeEpisodio = scanner.nextLine();
        List<Episodio> episodiosEncontrados = repository.episodioPorNome(nomeEpisodio);
        episodiosEncontrados.forEach(e ->
                System.out.printf("Série: %s Temporada %s - Episódio %s - %s\n",
                        e.getSerie().getTitulo(), e.getTemporada(),
                        e.getNumeroEpisodio(), e.getTitulo()));
    }

    private void buscarTop5Episodios() {
        buscarSeriePorTitulo();

        if(serieBuscada.isPresent()){
            Serie serie = serieBuscada.get();
            List<Episodio> topEpisodios = repository.topEpisodiosPorSerie(serie);
            topEpisodios.forEach(e ->
                    System.out.printf("Série: %s Temporada %s - Episódio %s - %s Avaliação %s\n",
                            e.getSerie().getTitulo(), e.getTemporada(),
                            e.getNumeroEpisodio(), e.getTitulo(), e.getAvaliacao() ));
        }
    }
}