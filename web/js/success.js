/* ═══════════════════════════════════════════════════════════
   success.js — Active Learning Inc.
   Client-side logic for success.jsp (Admin / Instructor / Student)
   ═══════════════════════════════════════════════════════════ */
 
'use strict';
 
/* ── Modal helpers ───────────────────────────────────────────────────────── */
 
/**
 * Open a modal overlay by its element id.
 * @param {string} id - The id of the .modal-overlay element.
 */
function openModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.add('open');
}
 
/**
 * Close a modal overlay by its element id.
 * @param {string} id - The id of the .modal-overlay element.
 */
function closeModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.remove('open');
}
 
/* ── Close modal on backdrop click ──────────────────────────────────────── */
document.querySelectorAll('.modal-overlay').forEach(function (overlay) {
  overlay.addEventListener('click', function (e) {
    if (e.target === overlay) {
      overlay.classList.remove('open');
    }
  });
});
 
/* ── Close modal on Escape key ───────────────────────────────────────────── */
document.addEventListener('keydown', function (e) {
  if (e.key === 'Escape') {
    document.querySelectorAll('.modal-overlay.open').forEach(function (overlay) {
      overlay.classList.remove('open');
    });
  }
});
 
/* ── Auto-dismiss toast notification ─────────────────────────────────────── */
(function () {
  var toast = document.querySelector('.toast');
  if (toast) {
    setTimeout(function () {
      toast.style.opacity = '0';
      /* Remove from DOM after fade so it doesn't affect layout */
      setTimeout(function () {
        toast.style.display = 'none';
      }, 500);
    }, 4000);
  }
})();

// fill the user edit modal
function openEditUserModal(id, username, role) {
    document.querySelector('#editUserModal input[name="editUserId"]').value = id;
    document.querySelector('#editUserModal input[name="editUsername"]').value = username;
    document.querySelector('#editUserModal select[name="editRole"]').value = role;
    document.querySelector('#editUserModal input[name="editPassword"]').value = '';
    // the password is set to be blank to keep privacy and for safe practice to keep sensitive information hidden
    
    openModal('editUserModal');
}

// fill the course edit modal
function openEditCourseModal(id, title, date) {
    document.querySelector('#editCourseModal input[name="editCourseId"]').value = id;
    document.querySelector('#editCourseModal input[name="editCourseTitle"]').value = title;
    document.querySelector('#editCourseModal input[name="editCourseDate"]').value = date;
    
    openModal('editCourseModal');
}