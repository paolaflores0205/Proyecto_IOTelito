package com.example.proyecto_iotelito.ui.superadmin.usuarios;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.ItemSuperadminUserBinding;
import com.example.proyecto_iotelito.model.superadmin.AdminUser;

import java.util.ArrayList;
import java.util.List;

public class UsuariosAdapter extends RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder> {
    public interface OnUserClickListener { void onUserClick(AdminUser user); }

    private final List<AdminUser> users = new ArrayList<>();
    private final OnUserClickListener listener;

    public UsuariosAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<AdminUser> newUsers) {
        users.clear();
        users.addAll(newUsers);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSuperadminUserBinding binding = ItemSuperadminUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new UsuarioViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() { return users.size(); }

    class UsuarioViewHolder extends RecyclerView.ViewHolder {
        private final ItemSuperadminUserBinding binding;

        UsuarioViewHolder(ItemSuperadminUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AdminUser user) {
            binding.tvUserInitials.setText(initials(user.name));
            binding.tvUserName.setText(user.name);
            binding.tvUserEmail.setText(user.email);
            binding.tvUserRole.setText(user.role);
            binding.tvUserRegistered.setText(itemView.getContext().getString(
                    R.string.superadmin_registered_format, user.registeredAt));
            binding.tvUserStatus.setText(user.active
                    ? R.string.superadmin_filter_active : R.string.superadmin_filter_inactive);
            binding.tvUserStatus.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    user.active ? R.color.io_success_text : R.color.io_danger_text));
            binding.btnUserDetail.setOnClickListener(v -> listener.onUserClick(user));
            binding.getRoot().setOnClickListener(v -> listener.onUserClick(user));
        }
    }

    private String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0) return "?";
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }
}
