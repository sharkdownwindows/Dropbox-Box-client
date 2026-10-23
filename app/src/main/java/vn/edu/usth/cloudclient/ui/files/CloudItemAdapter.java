package vn.edu.usth.cloudclient.ui.files;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import vn.edu.usth.cloudclient.R;
import vn.edu.usth.cloudclient.databinding.ItemCloudFileBinding;
import vn.edu.usth.cloudclient.model.CloudItem;

public final class CloudItemAdapter extends ListAdapter<CloudItem, CloudItemAdapter.Holder> {
    public interface Listener { void onClick(CloudItem item); }
    private final Listener listener;

    public CloudItemAdapter(Listener listener) {
        super(new DiffUtil.ItemCallback<CloudItem>() {
            @Override public boolean areItemsTheSame(@NonNull CloudItem a, @NonNull CloudItem b) {
                return a.id.equals(b.id);
            }
            @Override public boolean areContentsTheSame(@NonNull CloudItem a, @NonNull CloudItem b) {
                return a.name.equals(b.name) && a.folder == b.folder && a.sizeBytes == b.sizeBytes;
            }
        });
        this.listener = listener;
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(ItemCloudFileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        CloudItem item = getItem(position);
        holder.binding.name.setText(item.name);
        holder.binding.detail.setText(item.folder
            ? holder.itemView.getContext().getString(R.string.folder)
            : holder.itemView.getContext().getString(R.string.file_bytes, item.sizeBytes));
        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemCloudFileBinding binding;
        Holder(ItemCloudFileBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
}
