# =============================================================
# Script de Criacao da Estrutura do Projeto ERP-Desktop (C#/WPF)
# Execute com: powershell -ExecutionPolicy Bypass -File setup.ps1
# =============================================================

$base = "$PSScriptRoot\ERP-Desktop"

# Cria diretorios
$dirs = @(
    "$base",
    "$base\Models",
    "$base\ViewModels",
    "$base\Views",
    "$base\Data",
    "$base\Services",
    "$base\Resources"
)
foreach ($d in $dirs) {
    if (-not (Test-Path $d)) { New-Item -ItemType Directory -Path $d | Out-Null }
}
Write-Host "[OK] Diretorios criados" -ForegroundColor Green

# ---- ERP-Desktop.csproj ----
Set-Content "$base\ERP-Desktop.csproj" @'
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <OutputType>WinExe</OutputType>
    <TargetFramework>net8.0-windows</TargetFramework>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
    <UseWPF>true</UseWPF>
    <AssemblyName>ERP-Desktop</AssemblyName>
    <RootNamespace>ERPDesktop</RootNamespace>
  </PropertyGroup>
  <ItemGroup>
    <PackageReference Include="Microsoft.EntityFrameworkCore.Sqlite" Version="8.0.0" />
    <PackageReference Include="Microsoft.EntityFrameworkCore.Tools" Version="8.0.0">
      <PrivateAssets>all</PrivateAssets>
    </PackageReference>
    <PackageReference Include="CommunityToolkit.Mvvm" Version="8.2.2" />
    <PackageReference Include="MaterialDesignThemes" Version="5.1.0" />
  </ItemGroup>
</Project>
'@

# ---- App.xaml ----
Set-Content "$base\App.xaml" @'
<Application x:Class="ERPDesktop.App"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
             StartupUri="Views/MainWindow.xaml">
  <Application.Resources>
    <ResourceDictionary>
      <ResourceDictionary.MergedDictionaries>
        <materialDesign:BundledTheme BaseTheme="Light" PrimaryColor="Blue" SecondaryColor="Amber"
            xmlns:materialDesign="clr-namespace:MaterialDesignThemes.Wpf;assembly=MaterialDesignThemes.Wpf"/>
        <ResourceDictionary Source="pack://application:,,,/MaterialDesignThemes.Wpf;component/Themes/MaterialDesign2.Defaults.xaml"/>
      </ResourceDictionary.MergedDictionaries>
    </ResourceDictionary>
  </Application.Resources>
</Application>
'@

# ---- App.xaml.cs ----
Set-Content "$base\App.xaml.cs" @'
using ERPDesktop.Data;
using Microsoft.EntityFrameworkCore;
using System.Windows;

namespace ERPDesktop;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        using var db = new ErpContext();
        db.Database.EnsureCreated();
    }
}
'@

# ---- Models/Cliente.cs ----
Set-Content "$base\Models\Cliente.cs" @'
namespace ERPDesktop.Models;

public class Cliente
{
    public int Id { get; set; }
    public string Nome { get; set; } = string.Empty;
    public string CPF_CNPJ { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string Telefone { get; set; } = string.Empty;
    public string Endereco { get; set; } = string.Empty;
    public DateTime DataCadastro { get; set; } = DateTime.Now;
    public bool Ativo { get; set; } = true;

    public ICollection<Venda> Vendas { get; set; } = new List<Venda>();
}
'@

# ---- Models/Produto.cs ----
Set-Content "$base\Models\Produto.cs" @'
namespace ERPDesktop.Models;

public class Produto
{
    public int Id { get; set; }
    public string Nome { get; set; } = string.Empty;
    public string Descricao { get; set; } = string.Empty;
    public string Codigo { get; set; } = string.Empty;
    public decimal Preco { get; set; }
    public int Estoque { get; set; }
    public int EstoqueMinimo { get; set; } = 5;
    public string Categoria { get; set; } = string.Empty;
    public bool Ativo { get; set; } = true;

    public bool EstoqueBaixo => Estoque <= EstoqueMinimo;
    public ICollection<ItemVenda> Itens { get; set; } = new List<ItemVenda>();
}
'@

# ---- Models/Venda.cs ----
Set-Content "$base\Models\Venda.cs" @'
namespace ERPDesktop.Models;

public class Venda
{
    public int Id { get; set; }
    public int ClienteId { get; set; }
    public Cliente? Cliente { get; set; }
    public DateTime DataVenda { get; set; } = DateTime.Now;
    public decimal Total { get; set; }
    public string Status { get; set; } = "Pendente";
    public string FormaPagamento { get; set; } = "Dinheiro";
    public string Observacoes { get; set; } = string.Empty;

    public ICollection<ItemVenda> Itens { get; set; } = new List<ItemVenda>();
}
'@

# ---- Models/ItemVenda.cs ----
Set-Content "$base\Models\ItemVenda.cs" @'
namespace ERPDesktop.Models;

public class ItemVenda
{
    public int Id { get; set; }
    public int VendaId { get; set; }
    public Venda? Venda { get; set; }
    public int ProdutoId { get; set; }
    public Produto? Produto { get; set; }
    public int Quantidade { get; set; }
    public decimal PrecoUnitario { get; set; }
    public decimal Subtotal => Quantidade * PrecoUnitario;
}
'@

# ---- Models/ContaFinanceira.cs ----
Set-Content "$base\Models\ContaFinanceira.cs" @'
namespace ERPDesktop.Models;

public class ContaFinanceira
{
    public int Id { get; set; }
    public string Descricao { get; set; } = string.Empty;
    public decimal Valor { get; set; }
    public string Tipo { get; set; } = "Receita"; // Receita ou Despesa
    public DateTime DataVencimento { get; set; }
    public DateTime? DataPagamento { get; set; }
    public string Status { get; set; } = "Aberto"; // Aberto, Pago, Vencido
    public string Categoria { get; set; } = string.Empty;
}
'@

# ---- Data/ErpContext.cs ----
Set-Content "$base\Data\ErpContext.cs" @'
using ERPDesktop.Models;
using Microsoft.EntityFrameworkCore;

namespace ERPDesktop.Data;

public class ErpContext : DbContext
{
    public DbSet<Cliente> Clientes { get; set; }
    public DbSet<Produto> Produtos { get; set; }
    public DbSet<Venda> Vendas { get; set; }
    public DbSet<ItemVenda> ItensVenda { get; set; }
    public DbSet<ContaFinanceira> ContasFinanceiras { get; set; }

    protected override void OnConfiguring(DbContextOptionsBuilder options)
        => options.UseSqlite("Data Source=erp.db");

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Venda>()
            .HasOne(v => v.Cliente)
            .WithMany(c => c.Vendas)
            .HasForeignKey(v => v.ClienteId);

        modelBuilder.Entity<ItemVenda>()
            .HasOne(i => i.Venda)
            .WithMany(v => v.Itens)
            .HasForeignKey(i => i.VendaId);

        modelBuilder.Entity<ItemVenda>()
            .HasOne(i => i.Produto)
            .WithMany(p => p.Itens)
            .HasForeignKey(i => i.ProdutoId);
    }
}
'@

# ---- ViewModels/BaseViewModel.cs ----
Set-Content "$base\ViewModels\BaseViewModel.cs" @'
using CommunityToolkit.Mvvm.ComponentModel;

namespace ERPDesktop.ViewModels;

public abstract partial class BaseViewModel : ObservableObject
{
    [ObservableProperty]
    private bool _isLoading;

    [ObservableProperty]
    private string _statusMessage = string.Empty;
}
'@

# ---- ViewModels/ClientesViewModel.cs ----
Set-Content "$base\ViewModels\ClientesViewModel.cs" @'
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using ERPDesktop.Data;
using ERPDesktop.Models;
using Microsoft.EntityFrameworkCore;
using System.Collections.ObjectModel;

namespace ERPDesktop.ViewModels;

public partial class ClientesViewModel : BaseViewModel
{
    [ObservableProperty]
    private ObservableCollection<Cliente> _clientes = new();

    [ObservableProperty]
    private Cliente? _clienteSelecionado;

    [ObservableProperty]
    private string _busca = string.Empty;

    // Formulario
    [ObservableProperty] private string _nome = string.Empty;
    [ObservableProperty] private string _cpfCnpj = string.Empty;
    [ObservableProperty] private string _email = string.Empty;
    [ObservableProperty] private string _telefone = string.Empty;
    [ObservableProperty] private string _endereco = string.Empty;

    [RelayCommand]
    private async Task CarregarAsync()
    {
        IsLoading = true;
        using var db = new ErpContext();
        var lista = await db.Clientes
            .Where(c => c.Ativo && (string.IsNullOrEmpty(Busca) || c.Nome.Contains(Busca) || c.CPF_CNPJ.Contains(Busca)))
            .OrderBy(c => c.Nome)
            .ToListAsync();
        Clientes = new ObservableCollection<Cliente>(lista);
        IsLoading = false;
    }

    [RelayCommand]
    private async Task SalvarAsync()
    {
        if (string.IsNullOrWhiteSpace(Nome)) { StatusMessage = "Nome e obrigatorio."; return; }

        using var db = new ErpContext();
        if (ClienteSelecionado == null)
        {
            db.Clientes.Add(new Cliente { Nome = Nome, CPF_CNPJ = CpfCnpj, Email = Email, Telefone = Telefone, Endereco = Endereco });
        }
        else
        {
            var c = await db.Clientes.FindAsync(ClienteSelecionado.Id);
            if (c != null) { c.Nome = Nome; c.CPF_CNPJ = CpfCnpj; c.Email = Email; c.Telefone = Telefone; c.Endereco = Endereco; }
        }
        await db.SaveChangesAsync();
        StatusMessage = "Cliente salvo com sucesso!";
        LimparFormulario();
        await CarregarAsync();
    }

    [RelayCommand]
    private async Task ExcluirAsync()
    {
        if (ClienteSelecionado == null) return;
        using var db = new ErpContext();
        var c = await db.Clientes.FindAsync(ClienteSelecionado.Id);
        if (c != null) { c.Ativo = false; await db.SaveChangesAsync(); }
        StatusMessage = "Cliente removido.";
        await CarregarAsync();
    }

    partial void OnClienteSelecionadoChanged(Cliente? value)
    {
        if (value == null) return;
        Nome = value.Nome; CpfCnpj = value.CPF_CNPJ;
        Email = value.Email; Telefone = value.Telefone; Endereco = value.Endereco;
    }

    [RelayCommand]
    private void LimparFormulario()
    {
        ClienteSelecionado = null;
        Nome = CpfCnpj = Email = Telefone = Endereco = string.Empty;
        StatusMessage = string.Empty;
    }
}
'@

# ---- ViewModels/ProdutosViewModel.cs ----
Set-Content "$base\ViewModels\ProdutosViewModel.cs" @'
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using ERPDesktop.Data;
using ERPDesktop.Models;
using Microsoft.EntityFrameworkCore;
using System.Collections.ObjectModel;

namespace ERPDesktop.ViewModels;

public partial class ProdutosViewModel : BaseViewModel
{
    [ObservableProperty]
    private ObservableCollection<Produto> _produtos = new();

    [ObservableProperty]
    private Produto? _produtoSelecionado;

    [ObservableProperty] private string _nome = string.Empty;
    [ObservableProperty] private string _codigo = string.Empty;
    [ObservableProperty] private string _descricao = string.Empty;
    [ObservableProperty] private decimal _preco;
    [ObservableProperty] private int _estoque;
    [ObservableProperty] private int _estoqueMinimo = 5;
    [ObservableProperty] private string _categoria = string.Empty;

    [RelayCommand]
    private async Task CarregarAsync()
    {
        IsLoading = true;
        using var db = new ErpContext();
        var lista = await db.Produtos.Where(p => p.Ativo).OrderBy(p => p.Nome).ToListAsync();
        Produtos = new ObservableCollection<Produto>(lista);
        IsLoading = false;
    }

    [RelayCommand]
    private async Task SalvarAsync()
    {
        if (string.IsNullOrWhiteSpace(Nome)) { StatusMessage = "Nome e obrigatorio."; return; }

        using var db = new ErpContext();
        if (ProdutoSelecionado == null)
        {
            db.Produtos.Add(new Produto { Nome = Nome, Codigo = Codigo, Descricao = Descricao, Preco = Preco, Estoque = Estoque, EstoqueMinimo = EstoqueMinimo, Categoria = Categoria });
        }
        else
        {
            var p = await db.Produtos.FindAsync(ProdutoSelecionado.Id);
            if (p != null) { p.Nome = Nome; p.Codigo = Codigo; p.Descricao = Descricao; p.Preco = Preco; p.Estoque = Estoque; p.EstoqueMinimo = EstoqueMinimo; p.Categoria = Categoria; }
        }
        await db.SaveChangesAsync();
        StatusMessage = "Produto salvo!";
        LimparFormulario();
        await CarregarAsync();
    }

    [RelayCommand]
    private async Task ExcluirAsync()
    {
        if (ProdutoSelecionado == null) return;
        using var db = new ErpContext();
        var p = await db.Produtos.FindAsync(ProdutoSelecionado.Id);
        if (p != null) { p.Ativo = false; await db.SaveChangesAsync(); }
        await CarregarAsync();
    }

    partial void OnProdutoSelecionadoChanged(Produto? value)
    {
        if (value == null) return;
        Nome = value.Nome; Codigo = value.Codigo; Descricao = value.Descricao;
        Preco = value.Preco; Estoque = value.Estoque; EstoqueMinimo = value.EstoqueMinimo; Categoria = value.Categoria;
    }

    [RelayCommand]
    private void LimparFormulario()
    {
        ProdutoSelecionado = null;
        Nome = Codigo = Descricao = Categoria = string.Empty;
        Preco = 0; Estoque = 0; EstoqueMinimo = 5;
        StatusMessage = string.Empty;
    }
}
'@

# ---- ViewModels/DashboardViewModel.cs ----
Set-Content "$base\ViewModels\DashboardViewModel.cs" @'
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using ERPDesktop.Data;
using Microsoft.EntityFrameworkCore;

namespace ERPDesktop.ViewModels;

public partial class DashboardViewModel : BaseViewModel
{
    [ObservableProperty] private int _totalClientes;
    [ObservableProperty] private int _totalProdutos;
    [ObservableProperty] private int _vendasHoje;
    [ObservableProperty] private decimal _receitaHoje;
    [ObservableProperty] private int _produtosBaixoEstoque;
    [ObservableProperty] private int _contasVencidas;

    [RelayCommand]
    private async Task CarregarAsync()
    {
        IsLoading = true;
        using var db = new ErpContext();
        var hoje = DateTime.Today;

        TotalClientes = await db.Clientes.CountAsync(c => c.Ativo);
        TotalProdutos = await db.Produtos.CountAsync(p => p.Ativo);
        VendasHoje = await db.Vendas.CountAsync(v => v.DataVenda.Date == hoje);
        ReceitaHoje = await db.Vendas.Where(v => v.DataVenda.Date == hoje).SumAsync(v => (decimal?)v.Total) ?? 0;
        ProdutosBaixoEstoque = await db.Produtos.CountAsync(p => p.Ativo && p.Estoque <= p.EstoqueMinimo);
        ContasVencidas = await db.ContasFinanceiras.CountAsync(c => c.Status == "Aberto" && c.DataVencimento.Date < hoje);

        IsLoading = false;
    }
}
'@

# ---- ViewModels/MainViewModel.cs ----
Set-Content "$base\ViewModels\MainViewModel.cs" @'
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace ERPDesktop.ViewModels;

public partial class MainViewModel : ObservableObject
{
    public DashboardViewModel Dashboard { get; } = new();
    public ClientesViewModel Clientes { get; } = new();
    public ProdutosViewModel Produtos { get; } = new();

    [ObservableProperty]
    private object _viewAtual;

    public MainViewModel()
    {
        _viewAtual = Dashboard;
        _ = Dashboard.CarregarCommand.ExecuteAsync(null);
    }

    [RelayCommand] private void IrParaDashboard() { ViewAtual = Dashboard; _ = Dashboard.CarregarCommand.ExecuteAsync(null); }
    [RelayCommand] private void IrParaClientes() { ViewAtual = Clientes; _ = Clientes.CarregarCommand.ExecuteAsync(null); }
    [RelayCommand] private void IrParaProdutos() { ViewAtual = Produtos; _ = Produtos.CarregarCommand.ExecuteAsync(null); }
}
'@

# ---- Views/MainWindow.xaml ----
Set-Content "$base\Views\MainWindow.xaml" @'
<Window x:Class="ERPDesktop.Views.MainWindow"
        xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
        xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
        xmlns:md="http://materialdesigninxaml.net/winfx/xaml/themes"
        xmlns:vm="clr-namespace:ERPDesktop.ViewModels"
        xmlns:views="clr-namespace:ERPDesktop.Views"
        Title="ERP Desktop" Height="700" Width="1100"
        WindowStartupLocation="CenterScreen"
        Background="{DynamicResource MaterialDesignPaper}"
        TextElement.Foreground="{DynamicResource MaterialDesignBody}">

  <Window.DataContext>
    <vm:MainViewModel/>
  </Window.DataContext>

  <Window.Resources>
    <DataTemplate DataType="{x:Type vm:DashboardViewModel}">
      <views:DashboardView/>
    </DataTemplate>
    <DataTemplate DataType="{x:Type vm:ClientesViewModel}">
      <views:ClientesView/>
    </DataTemplate>
    <DataTemplate DataType="{x:Type vm:ProdutosViewModel}">
      <views:ProdutosView/>
    </DataTemplate>
  </Window.Resources>

  <Grid>
    <Grid.ColumnDefinitions>
      <ColumnDefinition Width="200"/>
      <ColumnDefinition Width="*"/>
    </Grid.ColumnDefinitions>

    <!-- Menu Lateral -->
    <Border Background="{DynamicResource PrimaryHueDarkBrush}" Grid.Column="0">
      <StackPanel Margin="0,20,0,0">
        <TextBlock Text="ERP Desktop" FontSize="18" FontWeight="Bold"
                   Foreground="White" Margin="16,0,0,24" HorizontalAlignment="Left"/>

        <Button Style="{StaticResource MaterialDesignFlatButton}"
                Command="{Binding IrParaDashboardCommand}"
                Foreground="White" HorizontalAlignment="Stretch" HorizontalContentAlignment="Left" Margin="4,2">
          <StackPanel Orientation="Horizontal">
            <md:PackIcon Kind="ViewDashboard" Margin="0,0,8,0"/>
            <TextBlock Text="Dashboard"/>
          </StackPanel>
        </Button>

        <Button Style="{StaticResource MaterialDesignFlatButton}"
                Command="{Binding IrParaClientesCommand}"
                Foreground="White" HorizontalAlignment="Stretch" HorizontalContentAlignment="Left" Margin="4,2">
          <StackPanel Orientation="Horizontal">
            <md:PackIcon Kind="AccountGroup" Margin="0,0,8,0"/>
            <TextBlock Text="Clientes"/>
          </StackPanel>
        </Button>

        <Button Style="{StaticResource MaterialDesignFlatButton}"
                Command="{Binding IrParaProdutosCommand}"
                Foreground="White" HorizontalAlignment="Stretch" HorizontalContentAlignment="Left" Margin="4,2">
          <StackPanel Orientation="Horizontal">
            <md:PackIcon Kind="PackageVariant" Margin="0,0,8,0"/>
            <TextBlock Text="Produtos / Estoque"/>
          </StackPanel>
        </Button>
      </StackPanel>
    </Border>

    <!-- Conteudo Principal -->
    <ContentControl Grid.Column="1" Content="{Binding ViewAtual}" Margin="16"/>
  </Grid>
</Window>
'@

# ---- Views/MainWindow.xaml.cs ----
Set-Content "$base\Views\MainWindow.xaml.cs" @'
using System.Windows;

namespace ERPDesktop.Views;

public partial class MainWindow : Window
{
    public MainWindow() => InitializeComponent();
}
'@

# ---- Views/DashboardView.xaml ----
Set-Content "$base\Views\DashboardView.xaml" @'
<UserControl x:Class="ERPDesktop.Views.DashboardView"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
             xmlns:md="http://materialdesigninxaml.net/winfx/xaml/themes">
  <ScrollViewer VerticalScrollBarVisibility="Auto">
    <StackPanel>
      <TextBlock Text="Dashboard" FontSize="24" FontWeight="Bold" Margin="0,0,0,16"/>

      <UniformGrid Columns="3" Rows="2">
        <!-- Card: Clientes -->
        <md:Card Margin="8" Padding="16">
          <StackPanel>
            <md:PackIcon Kind="AccountGroup" Width="40" Height="40" Foreground="{DynamicResource PrimaryHueMidBrush}"/>
            <TextBlock Text="{Binding TotalClientes}" FontSize="32" FontWeight="Bold"/>
            <TextBlock Text="Clientes Ativos" Foreground="Gray"/>
          </StackPanel>
        </md:Card>

        <!-- Card: Produtos -->
        <md:Card Margin="8" Padding="16">
          <StackPanel>
            <md:PackIcon Kind="PackageVariant" Width="40" Height="40" Foreground="{DynamicResource PrimaryHueMidBrush}"/>
            <TextBlock Text="{Binding TotalProdutos}" FontSize="32" FontWeight="Bold"/>
            <TextBlock Text="Produtos Cadastrados" Foreground="Gray"/>
          </StackPanel>
        </md:Card>

        <!-- Card: Vendas Hoje -->
        <md:Card Margin="8" Padding="16">
          <StackPanel>
            <md:PackIcon Kind="CartOutline" Width="40" Height="40" Foreground="Green"/>
            <TextBlock Text="{Binding VendasHoje}" FontSize="32" FontWeight="Bold"/>
            <TextBlock Text="Vendas Hoje" Foreground="Gray"/>
          </StackPanel>
        </md:Card>

        <!-- Card: Receita Hoje -->
        <md:Card Margin="8" Padding="16">
          <StackPanel>
            <md:PackIcon Kind="CurrencyUsd" Width="40" Height="40" Foreground="Green"/>
            <TextBlock Text="{Binding ReceitaHoje, StringFormat=C}" FontSize="28" FontWeight="Bold"/>
            <TextBlock Text="Receita Hoje" Foreground="Gray"/>
          </StackPanel>
        </md:Card>

        <!-- Card: Estoque Baixo -->
        <md:Card Margin="8" Padding="16">
          <StackPanel>
            <md:PackIcon Kind="AlertCircle" Width="40" Height="40" Foreground="Orange"/>
            <TextBlock Text="{Binding ProdutosBaixoEstoque}" FontSize="32" FontWeight="Bold" Foreground="Orange"/>
            <TextBlock Text="Produtos com Estoque Baixo" Foreground="Gray"/>
          </StackPanel>
        </md:Card>

        <!-- Card: Contas Vencidas -->
        <md:Card Margin="8" Padding="16">
          <StackPanel>
            <md:PackIcon Kind="AlertOctagon" Width="40" Height="40" Foreground="Red"/>
            <TextBlock Text="{Binding ContasVencidas}" FontSize="32" FontWeight="Bold" Foreground="Red"/>
            <TextBlock Text="Contas Vencidas" Foreground="Gray"/>
          </StackPanel>
        </md:Card>
      </UniformGrid>
    </StackPanel>
  </ScrollViewer>
</UserControl>
'@

# ---- Views/DashboardView.xaml.cs ----
Set-Content "$base\Views\DashboardView.xaml.cs" @'
using System.Windows.Controls;

namespace ERPDesktop.Views;

public partial class DashboardView : UserControl
{
    public DashboardView() => InitializeComponent();
}
'@

# ---- Views/ClientesView.xaml ----
Set-Content "$base\Views\ClientesView.xaml" @'
<UserControl x:Class="ERPDesktop.Views.ClientesView"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
             xmlns:md="http://materialdesigninxaml.net/winfx/xaml/themes">
  <Grid>
    <Grid.RowDefinitions>
      <RowDefinition Height="Auto"/>
      <RowDefinition Height="*"/>
    </Grid.RowDefinitions>
    <Grid.ColumnDefinitions>
      <ColumnDefinition Width="*"/>
      <ColumnDefinition Width="320"/>
    </Grid.ColumnDefinitions>

    <TextBlock Text="Clientes" FontSize="24" FontWeight="Bold" Margin="0,0,0,8" Grid.ColumnSpan="2"/>

    <!-- Lista de Clientes -->
    <Grid Grid.Row="1" Grid.Column="0" Margin="0,0,8,0">
      <Grid.RowDefinitions>
        <RowDefinition Height="Auto"/>
        <RowDefinition Height="*"/>
      </Grid.RowDefinitions>
      <TextBox md:HintAssist.Hint="Buscar por nome ou CPF/CNPJ..."
               Text="{Binding Busca, UpdateSourceTrigger=PropertyChanged}"
               Margin="0,0,0,8" Style="{StaticResource MaterialDesignOutlinedTextBox}"/>
      <DataGrid Grid.Row="1" ItemsSource="{Binding Clientes}"
                SelectedItem="{Binding ClienteSelecionado}"
                AutoGenerateColumns="False" IsReadOnly="True" CanUserAddRows="False">
        <DataGrid.Columns>
          <DataGridTextColumn Header="Nome" Binding="{Binding Nome}" Width="*"/>
          <DataGridTextColumn Header="CPF/CNPJ" Binding="{Binding CPF_CNPJ}" Width="140"/>
          <DataGridTextColumn Header="Telefone" Binding="{Binding Telefone}" Width="120"/>
          <DataGridTextColumn Header="Email" Binding="{Binding Email}" Width="180"/>
        </DataGrid.Columns>
      </DataGrid>
    </Grid>

    <!-- Formulario -->
    <md:Card Grid.Row="1" Grid.Column="1" Padding="16">
      <StackPanel>
        <TextBlock Text="Cadastro de Cliente" FontSize="16" FontWeight="Bold" Margin="0,0,0,12"/>
        <TextBox md:HintAssist.Hint="Nome *" Text="{Binding Nome, UpdateSourceTrigger=PropertyChanged}"
                 Style="{StaticResource MaterialDesignOutlinedTextBox}" Margin="0,4"/>
        <TextBox md:HintAssist.Hint="CPF / CNPJ" Text="{Binding CpfCnpj, UpdateSourceTrigger=PropertyChanged}"
                 Style="{StaticResource MaterialDesignOutlinedTextBox}" Margin="0,4"/>
        <TextBox md:HintAssist.Hint="Email" Text="{Binding Email, UpdateSourceTrigger=PropertyChanged}"
                 Style="{StaticResource MaterialDesignOutlinedTextBox}" Margin="0,4"/>
        <TextBox md:HintAssist.Hint="Telefone" Text="{Binding Telefone, UpdateSourceTrigger=PropertyChanged}"
                 Style="{StaticResource MaterialDesignOutlinedTextBox}" Margin="0,4"/>
        <TextBox md:HintAssist.Hint="Endereco" Text="{Binding Endereco, UpdateSourceTrigger=PropertyChanged}"
                 Style="{StaticResource MaterialDesignOutlinedTextBox}" Margin="0,4"/>

        <TextBlock Text="{Binding StatusMessage}" Foreground="Green" Margin="0,8" TextWrapping="Wrap"/>

        <StackPanel Orientation="Horizontal" Margin="0,8,0,0">
          <Button Content="Salvar" Command="{Binding SalvarCommand}"
                  Style="{StaticResource MaterialDesignRaisedButton}" Margin="0,0,8,0"/>
          <Button Content="Excluir" Command="{Binding ExcluirCommand}"
                  Style="{StaticResource MaterialDesignOutlinedButton}" Foreground="Red" Margin="0,0,8,0"/>
          <Button Content="Novo" Command="{Binding LimparFormularioCommand}"
                  Style="{StaticResource MaterialDesignFlatButton}"/>
        </StackPanel>
      </StackPanel>
    </md:Card>
  </Grid>
</UserControl>
'@

# ---- Views/ClientesView.xaml.cs ----
Set-Content "$base\Views\ClientesView.xaml.cs" @'
using System.Windows.Controls;

namespace ERPDesktop.Views;

public partial class ClientesView : UserControl
{
    public ClientesView() => InitializeComponent();
}
'@

# ---- Views/ProdutosView.xaml ----
Set-Content "$base\Views\ProdutosView.xaml" @'
<UserControl x:Class="ERPDesktop.Views.ProdutosView"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
             xmlns:md="http://materialdesigninxaml.net/winfx/xaml/themes">
  <Grid>
    <Grid.RowDefinitions>
      <RowDefinition Height="Auto"/>
      <RowDefinition Height="*"/>
    </Grid.RowDefinitions>
    <Grid.ColumnDefinitions>
      <ColumnDefinition Width="*"/>
      <ColumnDefinition Width="320"/>
    </Grid.ColumnDefinitions>

    <TextBlock Text="Produtos / Estoque" FontSize="24" FontWeight="Bold" Margin="0,0,0,8" Grid.ColumnSpan="2"/>

    <!-- Lista -->
    <DataGrid Grid.Row="1" Grid.Column="0" Margin="0,0,8,0"
              ItemsSource="{Binding Produtos}"
              SelectedItem="{Binding ProdutoSelecionado}"
              AutoGenerateColumns="False" IsReadOnly="True" CanUserAddRows="False">
      <DataGrid.Columns>
        <DataGridTextColumn Header="Codigo" Binding="{Binding Codigo}" Width="90"/>
        <DataGridTextColumn Header="Nome" Binding="{Binding Nome}" Width="*"/>
        <DataGridTextColumn Header="Categoria" Binding="{Binding Categoria}" Width="120"/>
        <DataGridTextColumn Header="Preco" Binding="{Binding Preco, StringFormat=C}" Width="90"/>
        <DataGridTextColumn Header="Estoque" Binding="{Binding Estoque}" Width="80"/>
        <DataGridTemplateColumn Header="Status" Width="90">
          <DataGridTemplateColumn.CellTemplate>
            <DataTemplate>
              <TextBlock Text="{Binding EstoqueBaixo, Converter={StaticResource BoolToEstoqueConverter}}"
                         Foreground="{Binding EstoqueBaixo, Converter={StaticResource BoolToCorConverter}}"/>
            </DataTemplate>
          </DataGridTemplateColumn.CellTemplate>
        </DataGridTemplateColumn>
      </DataGrid.Columns>
    </DataGrid>

    <!-- Formulario -->
    <md:Card Grid.Row="1" Grid.Column="1" Padding="16">
      <StackPanel>
        <TextBlock Text="Cadastro de Produto" FontSize="16" FontWeight="Bold" Margin="0,0,0,12"/>
        <TextBox md:HintAssist.Hint="Nome *" Text="{Binding Nome, UpdateSourceTrigger=PropertyChanged}"
                 Style="{StaticResource MaterialDesignOutlinedTextBox}" Margin="0,4"/>
        <TextBox md:HintAssist.Hint="Codigo" Text="{Binding Codigo, UpdateSourceTrigger=PropertyChanged}"
                 Style="{StaticResource MaterialDesignOutlinedTextBox}" Margin="0,4"/>
        <TextBox md:HintAssist.Hint="Categoria" Text="{Binding Categoria, UpdateSourceTrigger=PropertyChanged}"
                 Style="{StaticResource MaterialDesignOutlinedTextBox}" Margin="0,4"/>
        <TextBox md:HintAssist.Hint="Descricao" Text="{Binding Descricao, UpdateSourceTrigger=PropertyChanged}"
                 Style="{StaticResource MaterialDesignOutlinedTextBox}" Margin="0,4"/>
        <TextBox md:HintAssist.Hint="Preco" Text="{Binding Preco, UpdateSourceTrigger=PropertyChanged}"
                 Style="{StaticResource MaterialDesignOutlinedTextBox}" Margin="0,4"/>
        <TextBox md:HintAssist.Hint="Estoque Atual" Text="{Binding Estoque, UpdateSourceTrigger=PropertyChanged}"
                 Style="{StaticResource MaterialDesignOutlinedTextBox}" Margin="0,4"/>
        <TextBox md:HintAssist.Hint="Estoque Minimo" Text="{Binding EstoqueMinimo, UpdateSourceTrigger=PropertyChanged}"
                 Style="{StaticResource MaterialDesignOutlinedTextBox}" Margin="0,4"/>

        <TextBlock Text="{Binding StatusMessage}" Foreground="Green" Margin="0,8" TextWrapping="Wrap"/>

        <StackPanel Orientation="Horizontal" Margin="0,8,0,0">
          <Button Content="Salvar" Command="{Binding SalvarCommand}"
                  Style="{StaticResource MaterialDesignRaisedButton}" Margin="0,0,8,0"/>
          <Button Content="Excluir" Command="{Binding ExcluirCommand}"
                  Style="{StaticResource MaterialDesignOutlinedButton}" Foreground="Red" Margin="0,0,8,0"/>
          <Button Content="Novo" Command="{Binding LimparFormularioCommand}"
                  Style="{StaticResource MaterialDesignFlatButton}"/>
        </StackPanel>
      </StackPanel>
    </md:Card>
  </Grid>
</UserControl>
'@

# ---- Views/ProdutosView.xaml.cs ----
Set-Content "$base\Views\ProdutosView.xaml.cs" @'
using System.Windows.Controls;

namespace ERPDesktop.Views;

public partial class ProdutosView : UserControl
{
    public ProdutosView() => InitializeComponent();
}
'@

# ---- Services/Converters.cs ----
Set-Content "$base\Services\Converters.cs" @'
using System.Globalization;
using System.Windows.Data;
using System.Windows.Media;

namespace ERPDesktop.Services;

public class BoolToEstoqueConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        => (bool)value ? "Baixo" : "OK";
    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        => throw new NotImplementedException();
}

public class BoolToCorConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        => (bool)value ? new SolidColorBrush(Colors.OrangeRed) : new SolidColorBrush(Colors.Green);
    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        => throw new NotImplementedException();
}
'@

# ---- README.md ----
Set-Content "$PSScriptRoot\README.md" @'
# ERP Desktop - C# WPF (.NET 8)

Sistema ERP Desktop para gerenciamento de clientes, produtos e vendas.

## Tecnologias
- .NET 8 / WPF
- Entity Framework Core 8 + SQLite
- CommunityToolkit.Mvvm (MVVM)
- Material Design in XAML

## Modulos
- **Dashboard** — visao geral com indicadores
- **Clientes** — cadastro, edicao e busca de clientes
- **Produtos / Estoque** — controle de produtos com alerta de estoque baixo

## Como executar

### Pre-requisitos
- [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)
- Visual Studio 2022 ou VS Code com extensao C#

### Passos
```bash
cd ERP-Desktop
dotnet restore
dotnet run
```

### Ou abrir no Visual Studio
Abra `ERP-Desktop.sln` e pressione F5.

## Estrutura
```
ERP-Desktop/
├── Models/          # Entidades do banco de dados
├── ViewModels/      # Logica de apresentacao (MVVM)
├── Views/           # Telas XAML
├── Data/            # DbContext (Entity Framework)
└── Services/        # Conversores e servicos auxiliares
```
'@

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Projeto ERP-Desktop criado com sucesso!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Proximos passos:" -ForegroundColor Yellow
Write-Host "  1. Instale o .NET 8 SDK: https://dotnet.microsoft.com/download/dotnet/8.0"
Write-Host "  2. Entre na pasta: cd ERP-Desktop"
Write-Host "  3. Restaure pacotes: dotnet restore"
Write-Host "  4. Execute: dotnet run"
Write-Host ""
