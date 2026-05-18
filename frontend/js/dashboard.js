/* ================================================
   dashboard.js — Lógica do Dashboard
   ================================================ */

// Verificar autenticação
if (!Auth.requireAuth()) throw new Error('Não autenticado');

// Injetar nome do usuário na navbar
document.getElementById('navbar-username').textContent = Auth.getUsername();

// ===== Estado =====
let allBooks = [];
let currentBookId = null;

// ===== Elementos =====
const booksGrid      = document.getElementById('books-grid');
const emptyState     = document.getElementById('empty-state');
const searchInput    = document.getElementById('search-input');
const filterStatus   = document.getElementById('filter-status');
const bookModal      = document.getElementById('book-modal');
const detailModal    = document.getElementById('detail-modal');
const bookForm       = document.getElementById('book-form');

// ===== INIT =====
(async function init() {
  await Promise.all([loadStats(), loadBooks()]);
  setupEventListeners();
})();

// ===== LOADERS =====
async function loadStats() {
  try {
    const stats = await api.books.stats();
    document.getElementById('stat-total').textContent   = stats.total;
    document.getElementById('stat-reading').textContent = stats.reading;
    document.getElementById('stat-read').textContent    = stats.read;
    document.getElementById('stat-wishlist').textContent= stats.wishlist;
    document.getElementById('stat-avg').textContent     =
      stats.averageRating > 0 ? `${stats.averageRating}★` : '—';
  } catch { /* silencioso */ }
}

async function loadBooks() {
  booksGrid.innerHTML = '<p style="color:var(--text-muted);grid-column:1/-1;">Carregando...</p>';
  try {
    allBooks = await api.books.list();
    renderBooks(allBooks);
  } catch (err) {
    Toast.error('Erro ao carregar livros');
  }
}

// ===== RENDER =====
function renderBooks(books) {
  if (!books.length) {
    booksGrid.innerHTML = '';
    emptyState.style.display = 'block';
    return;
  }
  emptyState.style.display = 'none';

  const statusLabel = { WISHLIST: 'Lista de desejo', READING: 'Lendo', READ: 'Lido' };
  const statusClass = { WISHLIST: 'badge-wishlist', READING: 'badge-reading', READ: 'badge-read' };

  booksGrid.innerHTML = books.map(book => `
    <article class="book-card" role="listitem" tabindex="0"
             onclick="openDetail('${book.id}')"
             onkeydown="if(event.key==='Enter') openDetail('${book.id}')">
      ${book.coverUrl
        ? `<img class="book-cover" src="${book.coverUrl}" alt="Capa de ${book.title}" loading="lazy" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'" /><div class="book-cover-placeholder" style="display:none">📖</div>`
        : `<div class="book-cover-placeholder">📖</div>`
      }
      <div class="book-card-body">
        <div class="book-title">${escHtml(book.title)}</div>
        <div class="book-author">${escHtml(book.author)}</div>
        <div class="book-card-footer">
          <span class="badge ${statusClass[book.status] || 'badge-wishlist'}">
            ${statusLabel[book.status] || book.status}
          </span>
          ${book.rating ? `<span class="stars">${'⭐'.repeat(book.rating)}</span>` : ''}
        </div>
      </div>
    </article>
  `).join('');
}

// ===== FILTROS & BUSCA =====
let searchTimer;
searchInput.addEventListener('input', () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(applyFilters, 300);
});
filterStatus.addEventListener('change', applyFilters);

function applyFilters() {
  const q      = searchInput.value.toLowerCase().trim();
  const status = filterStatus.value;
  let filtered = allBooks;
  if (q)      filtered = filtered.filter(b => b.title.toLowerCase().includes(q) || b.author.toLowerCase().includes(q));
  if (status) filtered = filtered.filter(b => b.status === status);
  renderBooks(filtered);
}

// ===== MODAL NOVO/EDITAR LIVRO =====
document.getElementById('new-book-btn').addEventListener('click', openCreateModal);
document.getElementById('modal-close-btn').addEventListener('click', closeBookModal);
document.getElementById('modal-cancel-btn').addEventListener('click', closeBookModal);

function openCreateModal() {
  currentBookId = null;
  bookForm.reset();
  document.getElementById('book-id').value = '';
  document.getElementById('modal-title').textContent = 'Novo Livro';
  document.getElementById('book-save-btn').textContent = 'Salvar';
  clearFormErrors();
  openModal(bookModal);
}

function openEditModal(book) {
  currentBookId = book.id;
  document.getElementById('book-id').value    = book.id;
  document.getElementById('book-title').value  = book.title || '';
  document.getElementById('book-author').value = book.author || '';
  document.getElementById('book-isbn').value   = book.isbn || '';
  document.getElementById('book-genre').value  = book.genre || '';
  document.getElementById('book-status').value = book.status || 'WISHLIST';
  document.getElementById('book-rating').value = book.rating || '';
  document.getElementById('book-cover').value  = book.coverUrl || '';
  document.getElementById('book-notes').value  = book.notes || '';
  document.getElementById('modal-title').textContent = 'Editar Livro';
  document.getElementById('book-save-btn').textContent = 'Atualizar';
  clearFormErrors();
  closeModal(detailModal);
  openModal(bookModal);
}

function closeBookModal() { closeModal(bookModal); }

// ===== SUBMIT FORMULÁRIO =====
bookForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  clearFormErrors();

  const payload = {
    title:    document.getElementById('book-title').value.trim(),
    author:   document.getElementById('book-author').value.trim(),
    isbn:     document.getElementById('book-isbn').value.trim() || undefined,
    genre:    document.getElementById('book-genre').value.trim() || undefined,
    status:   document.getElementById('book-status').value,
    rating:   document.getElementById('book-rating').value ? parseInt(document.getElementById('book-rating').value) : undefined,
    coverUrl: document.getElementById('book-cover').value.trim() || undefined,
    notes:    document.getElementById('book-notes').value.trim() || undefined,
  };

  let valid = true;
  if (!payload.title)  { showFormError('book-title',  'book-title-error',  'Título é obrigatório'); valid = false; }
  if (!payload.author) { showFormError('book-author', 'book-author-error', 'Autor é obrigatório'); valid = false; }
  if (!valid) return;

  const btn = document.getElementById('book-save-btn');
  btn.disabled = true;
  btn.innerHTML = '<span class="loader"></span>';

  try {
    if (currentBookId) {
      await api.books.update(currentBookId, payload);
      Toast.success('Livro atualizado! ✅');
    } else {
      await api.books.create(payload);
      Toast.success('Livro adicionado! 📚');
    }
    closeBookModal();
    await Promise.all([loadStats(), loadBooks()]);
  } catch (err) {
    if (err.fields) {
      Object.entries(err.fields).forEach(([f, m]) => {
        const id = `book-${f}`;
        showFormError(id, `${id}-error`, m);
      });
    } else {
      Toast.error(err.message || 'Erro ao salvar livro');
    }
  } finally {
    btn.disabled = false;
    btn.textContent = currentBookId ? 'Atualizar' : 'Salvar';
  }
});

// ===== DETALHE DO LIVRO =====
async function openDetail(id) {
  try {
    const book = await api.books.get(id);
    currentBookId = id;

    const statusLabel = { WISHLIST: '📋 Lista de desejo', READING: '📖 Lendo', READ: '✅ Lido' };
    const stars = book.rating ? '⭐'.repeat(book.rating) + `  (${book.rating}/5)` : 'Sem avaliação';

    document.getElementById('detail-content').innerHTML = `
      <div class="book-detail">
        ${book.coverUrl
          ? `<img src="${book.coverUrl}" alt="Capa" class="book-detail-cover" onerror="this.style.display='none'" />`
          : `<div class="book-detail-cover">📖</div>`}
        <div class="book-detail-meta">
          <h2 class="book-detail-title">${escHtml(book.title)}</h2>
          <div class="meta-row"><span class="meta-label">Autor</span><span class="meta-value">${escHtml(book.author)}</span></div>
          ${book.isbn   ? `<div class="meta-row"><span class="meta-label">ISBN</span><span class="meta-value">${escHtml(book.isbn)}</span></div>` : ''}
          ${book.genre  ? `<div class="meta-row"><span class="meta-label">Gênero</span><span class="meta-value">${escHtml(book.genre)}</span></div>` : ''}
          <div class="meta-row"><span class="meta-label">Status</span><span class="meta-value">${statusLabel[book.status] || book.status}</span></div>
          <div class="meta-row"><span class="meta-label">Avaliação</span><span class="meta-value">${stars}</span></div>
          ${book.notes ? `<div style="margin-top:0.5rem;"><span class="meta-label">Notas</span><p style="margin-top:0.3rem;font-size:0.9rem;color:var(--text-secondary)">${escHtml(book.notes)}</p></div>` : ''}
        </div>
      </div>
    `;
    openModal(detailModal);

    document.getElementById('detail-edit-btn').onclick = () => openEditModal(book);
    document.getElementById('detail-delete-btn').onclick = () => confirmDelete(id);
  } catch (err) {
    Toast.error('Erro ao carregar livro');
  }
}

document.getElementById('detail-close-btn').addEventListener('click', () => closeModal(detailModal));

async function confirmDelete(id) {
  if (!confirm('Tem certeza que deseja excluir este livro?')) return;
  try {
    await api.books.delete(id);
    Toast.success('Livro excluído');
    closeModal(detailModal);
    await Promise.all([loadStats(), loadBooks()]);
  } catch (err) {
    Toast.error(err.message || 'Erro ao excluir livro');
  }
}

// ===== LOGOUT =====
document.getElementById('logout-btn').addEventListener('click', () => {
  if (confirm('Deseja sair?')) Auth.logout();
});

// ===== MODAL HELPERS =====
function openModal(modal)  { modal.classList.add('open'); document.body.style.overflow = 'hidden'; }
function closeModal(modal) { modal.classList.remove('open'); document.body.style.overflow = ''; }

// Fechar modal ao clicar no overlay
document.querySelectorAll('.modal-overlay').forEach(overlay => {
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) closeModal(overlay);
  });
});

// ===== FORM HELPERS =====
function clearFormErrors() {
  document.querySelectorAll('#book-form .field-error').forEach(el => { el.textContent = ''; el.classList.remove('visible'); });
  document.querySelectorAll('#book-form .form-input').forEach(el => el.classList.remove('error'));
}

function showFormError(inputId, errorId, message) {
  const input = document.getElementById(inputId);
  const error = document.getElementById(errorId);
  if (input) input.classList.add('error');
  if (error) { error.textContent = message; error.classList.add('visible'); }
}

function escHtml(str) {
  return String(str || '')
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

function setupEventListeners() {
  // Fechar modais com Escape
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      closeModal(bookModal);
      closeModal(detailModal);
    }
  });
}
